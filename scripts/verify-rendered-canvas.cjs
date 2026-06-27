const http = require('http');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { chromium } = require('playwright');

const ROOT = path.join(process.cwd(), 'work', 'build', 'web-run');
const OUT_DIR = path.join(process.cwd(), 'output', 'playwright');
const WAIT_MS = Number(process.env.WAIT_MS || 700000);
const VIEWPORT_WIDTH = Number(process.env.VIEWPORT_WIDTH || 1280);
const VIEWPORT_HEIGHT = Number(process.env.VIEWPORT_HEIGHT || 720);
const WORLD_NAME = String(process.env.WEBMC_WORLD_NAME || 'Web World');
const CAPTURE_TIMEOUT_MS = Number(process.env.CAPTURE_TIMEOUT_MS || 20000);
const CAPTURE_DATA_URL_TIMEOUT_MS = Number(process.env.CAPTURE_DATA_URL_TIMEOUT_MS || 15000);
const READ_STATE_TIMEOUT_MS = Number(process.env.READ_STATE_TIMEOUT_MS || 2500);
const HEARTBEAT_MS = Number(process.env.HEARTBEAT_MS || 15000);
const ALLOW_SLOW_CAPTURE = !/^(0|false|no|off)$/i.test(String(process.env.ALLOW_SLOW_CAPTURE || '1'));
const KEYBOARD_SMOKE = /^(1|true|yes|on)$/i.test(String(process.env.KEYBOARD_SMOKE || ''));
const MOUSE_SMOKE = /^(1|true|yes|on)$/i.test(String(process.env.MOUSE_SMOKE || ''));
const KEYBOARD_KEYS = String(process.env.KEYBOARD_KEYS || 'KeyW,Space,Escape')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.bin': 'application/octet-stream',
  '.vfs': 'application/octet-stream',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png'
};

function safeJoin(root, reqPath) {
  const cleaned = (reqPath || '/').split('?')[0].split('#')[0];
  const rel = cleaned === '/' ? '/index.html' : cleaned;
  const abs = path.normalize(path.join(root, rel));
  return abs.startsWith(root) ? abs : null;
}

function createServer(beacons) {
  return http.createServer((req, res) => {
    const parsed = new URL(req.url || '/', 'http://localhost');
    if (parsed.pathname === '/__webmc_state') {
      let state = null;
      const raw = parsed.searchParams.get('d');
      if (raw) {
        try {
          state = JSON.parse(raw);
        } catch {
          state = { parseError: true, raw };
        }
      }
      beacons.push({
        receivedAt: new Date().toISOString(),
        source: parsed.searchParams.get('source'),
        t: parsed.searchParams.get('t'),
        state
      });
      res.writeHead(204);
      res.end();
      return;
    }

    const filePath = safeJoin(ROOT, req.url || '/');
    if (!filePath) {
      res.writeHead(403);
      res.end('Forbidden');
      return;
    }
    fs.readFile(filePath, (err, data) => {
      if (err) {
        res.writeHead(404);
        res.end('Not Found');
        return;
      }
      const ext = path.extname(filePath).toLowerCase();
      res.writeHead(200, {
        'Content-Type': MIME[ext] || 'application/octet-stream',
        'Cache-Control': 'no-cache'
      });
      res.end(data);
    });
  });
}

function readChunks(buffer) {
  const chunks = [];
  let offset = 8;
  while (offset + 8 <= buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString('ascii', offset + 4, offset + 8);
    const dataStart = offset + 8;
    const dataEnd = dataStart + length;
    chunks.push({ type, data: buffer.subarray(dataStart, dataEnd) });
    offset = dataEnd + 4;
    if (type === 'IEND') break;
  }
  return chunks;
}

function unfilterPng(width, height, bpp, inflated) {
  const stride = width * bpp;
  const out = Buffer.alloc(stride * height);
  let inOffset = 0;
  let outOffset = 0;
  for (let y = 0; y < height; y++) {
    const filter = inflated[inOffset++];
    for (let x = 0; x < stride; x++) {
      const raw = inflated[inOffset++];
      const left = x >= bpp ? out[outOffset + x - bpp] : 0;
      const up = y > 0 ? out[outOffset + x - stride] : 0;
      const upLeft = y > 0 && x >= bpp ? out[outOffset + x - stride - bpp] : 0;
      let value;
      if (filter === 0) value = raw;
      else if (filter === 1) value = raw + left;
      else if (filter === 2) value = raw + up;
      else if (filter === 3) value = raw + Math.floor((left + up) / 2);
      else if (filter === 4) {
        const p = left + up - upLeft;
        const pa = Math.abs(p - left);
        const pb = Math.abs(p - up);
        const pc = Math.abs(p - upLeft);
        const predict = pa <= pb && pa <= pc ? left : pb <= pc ? up : upLeft;
        value = raw + predict;
      } else {
        throw new Error(`Unsupported PNG filter ${filter}`);
      }
      out[outOffset + x] = value & 255;
    }
    outOffset += stride;
  }
  return out;
}

function analyzePng(filePath) {
  const png = fs.readFileSync(filePath);
  if (png.length < 8 || png.toString('hex', 0, 8) !== '89504e470d0a1a0a') {
    throw new Error('Not a PNG file');
  }
  const chunks = readChunks(png);
  const ihdr = chunks.find((c) => c.type === 'IHDR');
  if (!ihdr) throw new Error('Missing IHDR');
  const width = ihdr.data.readUInt32BE(0);
  const height = ihdr.data.readUInt32BE(4);
  const bitDepth = ihdr.data[8];
  const colorType = ihdr.data[9];
  if (bitDepth !== 8 || ![2, 6].includes(colorType)) {
    throw new Error(`Unsupported PNG format bitDepth=${bitDepth} colorType=${colorType}`);
  }
  const bpp = colorType === 6 ? 4 : 3;
  const idat = Buffer.concat(chunks.filter((c) => c.type === 'IDAT').map((c) => c.data));
  const pixels = unfilterPng(width, height, bpp, zlib.inflateSync(idat));
  const total = width * height;
  const step = Math.max(1, Math.floor(total / 120000));
  let sampled = 0;
  let nonBlack = 0;
  let rSum = 0;
  let gSum = 0;
  let bSum = 0;
  let r2Sum = 0;
  let g2Sum = 0;
  let b2Sum = 0;
  const buckets = new Set();
  const bucketCounts = new Map();
  for (let i = 0; i < total; i += step) {
    const p = i * bpp;
    const r = pixels[p];
    const g = pixels[p + 1];
    const b = pixels[p + 2];
    sampled++;
    rSum += r;
    gSum += g;
    bSum += b;
    r2Sum += r * r;
    g2Sum += g * g;
    b2Sum += b * b;
    if (r + g + b > 12) nonBlack++;
    const bucket = `${r >> 4}:${g >> 4}:${b >> 4}`;
    buckets.add(bucket);
    bucketCounts.set(bucket, (bucketCounts.get(bucket) || 0) + 1);
  }
  const edgeStep = Math.max(1, Math.floor(Math.sqrt(total / 60000)));
  let edgeSamples = 0;
  let edgeHits = 0;
  for (let y = 0; y < height - edgeStep; y += edgeStep) {
    for (let x = 0; x < width - edgeStep; x += edgeStep) {
      const p = (y * width + x) * bpp;
      const pr = ((y + edgeStep) * width + x) * bpp;
      const pd = (y * width + x + edgeStep) * bpp;
      const dr = Math.abs(pixels[p] - pixels[pr]) + Math.abs(pixels[p + 1] - pixels[pr + 1]) + Math.abs(pixels[p + 2] - pixels[pr + 2]);
      const dd = Math.abs(pixels[p] - pixels[pd]) + Math.abs(pixels[p + 1] - pixels[pd + 1]) + Math.abs(pixels[p + 2] - pixels[pd + 2]);
      edgeSamples += 2;
      if (dr > 36) edgeHits++;
      if (dd > 36) edgeHits++;
    }
  }
  const avgR = rSum / sampled;
  const avgG = gSum / sampled;
  const avgB = bSum / sampled;
  const varianceRgb = [
    Math.max(0, r2Sum / sampled - avgR * avgR),
    Math.max(0, g2Sum / sampled - avgG * avgG),
    Math.max(0, b2Sum / sampled - avgB * avgB)
  ];
  return {
    width,
    height,
    sampled,
    nonBlackRatio: nonBlack / sampled,
    averageRgb: [
      Math.round(avgR),
      Math.round(avgG),
      Math.round(avgB)
    ],
    varianceRgb: varianceRgb.map((v) => Math.round(v)),
    quantizedColorBuckets: buckets.size,
    dominantBucketRatio: Math.max(...bucketCounts.values()) / sampled,
    edgeRatio: edgeSamples ? edgeHits / edgeSamples : 0,
    topBuckets: Array.from(bucketCounts.entries()).sort((a, b) => b[1] - a[1]).slice(0, 8)
  };
}

function isMeaningfulMinecraftScene(stats) {
  return !!(
    stats &&
    stats.nonBlackRatio > 0.05 &&
    stats.quantizedColorBuckets >= 4 &&
    stats.dominantBucketRatio < 0.98 &&
    stats.edgeRatio > 0.001
  );
}

function latestState(beacons) {
  for (let i = beacons.length - 1; i >= 0; i--) {
    if (beacons[i].state) return beacons[i].state;
  }
  return null;
}

function timeoutAfter(ms, message) {
  return new Promise((_, reject) => setTimeout(() => reject(new Error(message)), ms));
}

async function readLatestState(page, beacons) {
  const pageState = page
    ? await Promise.race([
      page.evaluate(() => {
      const holder = globalThis.__webmcLatestState;
      return holder && holder.state ? holder.state : null;
      }),
      timeoutAfter(READ_STATE_TIMEOUT_MS, 'readLatestState timed out')
    ]).catch(() => null)
    : null;
  return pageState || latestState(beacons);
}

function requiredRenderedSectionsForState(state) {
  const visible = Number(state && state.visibleSections || 0);
  if (!Number.isFinite(visible) || visible <= 0) {
    return 0;
  }
  const coverageTarget = Math.ceil(visible * 0.75);
  return Math.min(visible, Math.max(Math.min(8, visible), coverageTarget));
}

function isTerrainReady(state) {
  if (!state) {
    return false;
  }
  if (state.webTerrainReady) {
    return true;
  }
  const rendered = Number(state.renderedSections || 0);
  const required = Number(state.requiredRenderedSections || 0) || requiredRenderedSectionsForState(state);
  return !!(
    required > 0 &&
    rendered >= required &&
    state.hasRenderedAllSections
  );
}

function isWorldReady(consoleEvents, state) {
  if (consoleEvents.some((line) => line.includes('[mc-web/worldload] probe success:'))) {
    return true;
  }
  return !!(
    state &&
    state.gameLoadFinished &&
    state.levelPresent &&
    state.playerPresent &&
    isTerrainReady(state)
  );
}

function isSceneReady(state) {
  return !!(
    state &&
    state.gameLoadFinished &&
    state.levelPresent &&
    state.playerPresent &&
    state.worldRenderEligible &&
    isTerrainReady(state)
  );
}

function capturePathForMode(screenshotPath, mode) {
  const dir = path.dirname(screenshotPath);
  const ext = path.extname(screenshotPath) || '.png';
  const base = path.basename(screenshotPath, ext);
  return path.join(dir, `${base}-${mode}${ext}`);
}

function annotateCapture(stats, captureTarget, extra = {}) {
  return {
    ...stats,
    captureTarget,
    meaningful: isMeaningfulMinecraftScene(stats),
    ...extra
  };
}

async function captureCanvas(page, screenshotPath) {
  const canvas = page.locator('#canvas, canvas').first();
  const attempts = [];
  let dataUrlError = null;
  try {
    const dataUrl = await Promise.race([
      page.evaluate(() => {
        const target = document.querySelector('#canvas, canvas');
        return target && typeof target.toDataURL === 'function' ? target.toDataURL('image/png') : null;
      }),
      new Promise((_, reject) => setTimeout(() => reject(new Error('canvas dataURL capture timed out')), CAPTURE_DATA_URL_TIMEOUT_MS))
    ]);
    if (dataUrl && dataUrl.startsWith('data:image/png;base64,')) {
      const dataUrlPath = capturePathForMode(screenshotPath, 'dataurl');
      fs.writeFileSync(dataUrlPath, Buffer.from(dataUrl.slice('data:image/png;base64,'.length), 'base64'));
      const dataUrlStats = annotateCapture(analyzePng(dataUrlPath), 'canvas-dataurl', { path: dataUrlPath });
      attempts.push(dataUrlStats);
      if (isMeaningfulMinecraftScene(dataUrlStats)) {
        fs.copyFileSync(dataUrlPath, screenshotPath);
        return { ...dataUrlStats, attempts };
      }
      dataUrlError = new Error('canvas dataURL captured a non-meaningful frame');
    } else {
      dataUrlError = new Error('canvas dataURL returned empty result');
    }
  } catch (err) {
    dataUrlError = err;
    attempts.push({
      captureTarget: 'canvas-dataurl',
      error: String(dataUrlError && dataUrlError.message || dataUrlError)
    });
  }

  if (!ALLOW_SLOW_CAPTURE) {
    throw new Error('captureCanvas failed: dataUrl=' + String(dataUrlError && dataUrlError.message || dataUrlError));
  }

  try {
    await page.waitForSelector('#canvas, canvas', { state: 'attached', timeout: CAPTURE_TIMEOUT_MS });
    const canvasPath = capturePathForMode(screenshotPath, 'visible-canvas');
    await canvas.screenshot({ path: canvasPath, timeout: CAPTURE_TIMEOUT_MS });
    const canvasStats = annotateCapture(analyzePng(canvasPath), 'canvas', {
      path: canvasPath,
      captureTarget: 'canvas',
      dataUrlError: String(dataUrlError && dataUrlError.message || dataUrlError)
    });
    attempts.push(canvasStats);
    if (isMeaningfulMinecraftScene(canvasStats)) {
      fs.copyFileSync(canvasPath, screenshotPath);
      return { ...canvasStats, attempts };
    }
  } catch (err) {
    attempts.push({
      captureTarget: 'canvas',
      error: String(err && err.message || err)
    });
  }

  try {
    const pagePath = capturePathForMode(screenshotPath, 'page');
    await page.screenshot({ path: pagePath, timeout: CAPTURE_TIMEOUT_MS, animations: 'disabled' });
    const pageStats = annotateCapture(analyzePng(pagePath), 'page-fallback', {
      path: pagePath,
      dataUrlError: String(dataUrlError && dataUrlError.message || dataUrlError)
    });
    attempts.push(pageStats);
    if (isMeaningfulMinecraftScene(pageStats)) {
      fs.copyFileSync(pagePath, screenshotPath);
      return { ...pageStats, attempts };
    }
  } catch (pageErr) {
    attempts.push({
      captureTarget: 'page-fallback',
      error: String(pageErr && pageErr.message || pageErr)
    });
  }

  const best = attempts.find((attempt) => attempt && attempt.width) || null;
  if (best) {
    return {
      ...best,
      attempts,
      dataUrlError: String(dataUrlError && dataUrlError.message || dataUrlError)
    };
  }

  throw new Error(
    'captureCanvas failed: '
      + attempts.map((attempt) => `${attempt.captureTarget}=${attempt.error || 'non-meaningful'}`).join('; ')
  );
}

async function readErrorOverlay(page) {
  return page.evaluate(() => {
    const error = document.querySelector('#error');
    const boot = document.querySelector('#boot');
    const status = document.querySelector('#status');
    const canvas = document.querySelector('#canvas, canvas');
    return {
      errorClass: error ? error.className : null,
      errorDisplay: error ? getComputedStyle(error).display : null,
      errorText: error ? String(error.textContent || '').slice(0, 8000) : null,
      bootClass: boot ? boot.className : null,
      bootDisplay: boot ? getComputedStyle(boot).display : null,
      statusText: status ? String(status.textContent || '') : null,
      canvasFocused: !!(canvas && document.activeElement === canvas)
    };
  }).catch((err) => ({
    readError: String(err && err.stack || err)
  }));
}

async function runKeyboardSmoke(page) {
  const result = {
    enabled: KEYBOARD_SMOKE,
    keys: KEYBOARD_KEYS,
    before: null,
    after: null,
    errorsBefore: 0,
    errorsAfter: 0,
    screenshot: null
  };
  if (!KEYBOARD_SMOKE) {
    return result;
  }

  result.before = await readErrorOverlay(page);
  await page.locator('#canvas, canvas').first().click({ timeout: 30000 });
  for (const key of KEYBOARD_KEYS) {
    await page.keyboard.press(key);
    await page.waitForTimeout(250);
  }
  await page.waitForTimeout(1500);
  result.after = await readErrorOverlay(page);
  return result;
}

async function readMouseState(page) {
  return page.evaluate(() => {
    const canvas = document.querySelector('#canvas, canvas');
    const error = document.querySelector('#error');
    return {
      canvasFocused: !!(canvas && document.activeElement === canvas),
      pointerLockGranted: !!(canvas && document.pointerLockElement === canvas),
      cursorMode: canvas && canvas.__webmcCursorMode || null,
      hasCursorState: !!(canvas && canvas.__webmcCursor),
      cursor: canvas && canvas.__webmcCursor ? { x: canvas.__webmcCursor.x, y: canvas.__webmcCursor.y } : null,
      pendingPointerLock: !!(canvas && canvas.__webmcPointerLockPending),
      errorDisplay: error ? getComputedStyle(error).display : null,
      errorText: error ? String(error.textContent || '').slice(0, 8000) : null
    };
  }).catch((err) => ({
    readError: String(err && err.stack || err)
  }));
}

async function runMouseSmoke(page) {
  const result = {
    enabled: MOUSE_SMOKE,
    before: null,
    after: null,
    click: null,
    screenshot: null
  };
  if (!MOUSE_SMOKE) {
    return result;
  }

  result.before = await readMouseState(page);
  const canvas = page.locator('#canvas, canvas').first();
  await canvas.waitFor({ state: 'visible', timeout: 30000 });
  const box = await canvas.boundingBox();
  if (!box) {
    result.after = { errorDisplay: 'missing-canvas', errorText: 'Canvas bounding box was unavailable' };
    return result;
  }

  const x = Math.round(box.x + box.width / 2);
  const y = Math.round(box.y + box.height / 2);
  result.click = { x, y, width: box.width, height: box.height };
  await page.mouse.click(x, y);
  await page.waitForTimeout(500);
  await page.mouse.move(x + Math.round(box.width * 0.08), y + Math.round(box.height * 0.04), { steps: 6 });
  await page.mouse.move(x + Math.round(box.width * 0.14), y - Math.round(box.height * 0.03), { steps: 6 });
  await page.waitForTimeout(1000);
  result.after = await readMouseState(page);
  if (result.before && result.before.cursor && result.after && result.after.cursor) {
    const dx = result.after.cursor.x - result.before.cursor.x;
    const dy = result.after.cursor.y - result.before.cursor.y;
    result.after.cursorDelta = { x: dx, y: dy };
    result.after.cursorMoved = Math.abs(dx) + Math.abs(dy) > 0.5;
  }
  return result;
}

(async () => {
  if (!fs.existsSync(path.join(ROOT, 'index.html'))) {
    throw new Error(`Missing web-run build at ${ROOT}. Run npm run phase197:build first.`);
  }
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const beacons = [];
  const consoleEvents = [];
  const pageErrors = [];
  const requestFailures = [];
  const server = createServer(beacons);
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const url = `http://127.0.0.1:${port}/?boot=mcMain&autostart=1&world=${encodeURIComponent(WORLD_NAME)}&t=${Date.now()}`;
  const startedAt = new Date().toISOString();

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT } });
  page.on('console', (msg) => {
    const text = msg.text();
    consoleEvents.push(text);
    if (
      text.includes('[mc-web/worldload]') ||
      text.includes('[mc-web/occlusion]') ||
      text.includes('[mc-web/setupRender]') ||
      text.includes('[mc-web/compileSections]') ||
      text.includes('[mc-web/sectionMesh]') ||
      text.includes('[mc-web/sectionCompile]') ||
      text.includes('[mc-web/renderGate/state]') ||
      text.includes('[mc-web/render/state]') ||
      text.includes('[mc-web/diag]') ||
      text.includes('[mc-web/gl]') ||
      text.includes('[mc-web/chunkgen]') ||
      text.includes('[mc-web/serverchunks]') ||
      text.includes('[mc-web/clientpkt]') ||
      text.includes('[mc-web/chunks]') ||
      text.includes('[ERROR]')
    ) {
      console.log(text);
    }
  });
  page.on('pageerror', (err) => pageErrors.push(String(err && err.stack || err)));
  page.on('requestfailed', (req) => requestFailures.push({ url: req.url(), error: req.failure() && req.failure().errorText }));

  let screenshotStats = null;
  let screenshotPath = path.join(OUT_DIR, 'render-canvas-latest.png');
  let worldReadyAtMs = null;
  let sceneReadyAtMs = null;
  let firstNonBlackAtMs = null;
  let firstMeaningfulSceneAtMs = null;
  let keyboardSmoke = { enabled: KEYBOARD_SMOKE };
  let mouseSmoke = { enabled: MOUSE_SMOKE };
  let runError = null;
  const heartbeatPath = path.join(OUT_DIR, 'render-canvas-heartbeat-latest.json');
  let lastHeartbeatAt = 0;
  function writeHeartbeat(startMs, state, note) {
    const elapsedMs = Date.now() - startMs;
    const heartbeat = {
      schemaVersion: 1,
      startedAt,
      updatedAt: new Date().toISOString(),
      elapsedMs,
      url,
      note,
      worldReadyAtMs,
      sceneReadyAtMs,
      firstNonBlackAtMs,
      firstMeaningfulSceneAtMs,
      latestState: state || latestState(beacons),
      lastBeacon: beacons.length ? beacons[beacons.length - 1] : null,
      screenshotStats,
      console: {
        errors: consoleEvents.filter((line) => line.includes('[ERROR]') || line.includes('[mc-web/gl]')).slice(-40),
        tail: consoleEvents.slice(-80)
      },
      pageErrors: pageErrors.slice(-20),
      requestFailures: requestFailures.slice(-20)
    };
    fs.writeFileSync(heartbeatPath, JSON.stringify(heartbeat, null, 2));
    console.log(
      `render.heartbeat elapsedMs=${elapsedMs} note=${note} screen=${heartbeat.latestState && heartbeat.latestState.screen} rendered=${heartbeat.latestState && heartbeat.latestState.renderedSections}/${heartbeat.latestState && heartbeat.latestState.requiredRenderedSections} terrain=${heartbeat.latestState && heartbeat.latestState.webTerrainReady}`
    );
    lastHeartbeatAt = Date.now();
  }
  try {
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 90000 });
    const startMs = Date.now();
    while (Date.now() - startMs < WAIT_MS) {
      const state = await readLatestState(page, beacons);
      if (worldReadyAtMs == null && isWorldReady(consoleEvents, state)) {
        worldReadyAtMs = Date.now() - startMs;
      }
      if (sceneReadyAtMs == null && isSceneReady(state)) {
        sceneReadyAtMs = Date.now() - startMs;
      }
      if (sceneReadyAtMs != null) {
        await page.waitForTimeout(8000);
        screenshotStats = await captureCanvas(page, screenshotPath);
        if (firstNonBlackAtMs == null && screenshotStats.nonBlackRatio > 0.05) {
          firstNonBlackAtMs = Date.now() - startMs;
        }
        if (isMeaningfulMinecraftScene(screenshotStats)) {
          firstMeaningfulSceneAtMs = Date.now() - startMs;
          break;
        }
      }
      if (Date.now() - lastHeartbeatAt >= HEARTBEAT_MS) {
        writeHeartbeat(startMs, state, state ? 'waiting' : 'state-unavailable');
      }
      await page.waitForTimeout(1000);
    }
    if (!screenshotStats) {
      screenshotStats = await captureCanvas(page, screenshotPath);
    }
    if (screenshotStats && isMeaningfulMinecraftScene(screenshotStats)) {
      keyboardSmoke = await runKeyboardSmoke(page);
      mouseSmoke = await runMouseSmoke(page);
    }
  } catch (err) {
    runError = String(err && err.stack || err);
    if (!screenshotStats) {
      try {
        screenshotStats = await captureCanvas(page, screenshotPath);
      } catch (captureErr) {
        requestFailures.push({
          url: 'capture',
          error: String(captureErr && captureErr.stack || captureErr)
        });
      }
    }
  }

  const finalState = await readLatestState(page, beacons);
  if (keyboardSmoke && keyboardSmoke.enabled) {
    keyboardSmoke.errorsBefore = pageErrors.length;
    const keyboardScreenshotPath = path.join(OUT_DIR, 'render-keyboard-smoke-latest.png');
    try {
      keyboardSmoke.screenshot = keyboardScreenshotPath;
      keyboardSmoke.screenshotStats = await captureCanvas(page, keyboardScreenshotPath);
    } catch (err) {
      keyboardSmoke.screenshotError = String(err && err.stack || err);
    }
  }
  if (mouseSmoke && mouseSmoke.enabled) {
    mouseSmoke.errorsAfter = pageErrors.length;
    const mouseScreenshotPath = path.join(OUT_DIR, 'render-mouse-smoke-latest.png');
    try {
      mouseSmoke.screenshot = mouseScreenshotPath;
      mouseSmoke.screenshotStats = await captureCanvas(page, mouseScreenshotPath);
    } catch (err) {
      mouseSmoke.screenshotError = String(err && err.stack || err);
    }
  }
  await browser.close().catch(() => {});
  await new Promise((resolve) => server.close(resolve)).catch(() => {});

  const keyboardPass = !keyboardSmoke || !keyboardSmoke.enabled || !!(
    keyboardSmoke.after &&
    keyboardSmoke.after.errorDisplay === 'none' &&
    !keyboardSmoke.after.errorText &&
    pageErrors.length === 0
  );
  const mousePass = !mouseSmoke || !mouseSmoke.enabled || !!(
    mouseSmoke.after &&
    (mouseSmoke.after.canvasFocused || mouseSmoke.after.cursorMoved || mouseSmoke.after.pointerLockGranted) &&
    mouseSmoke.after.errorDisplay === 'none' &&
    !mouseSmoke.after.errorText &&
    pageErrors.length === 0
  );
  const pass = !!(
    worldReadyAtMs != null &&
    sceneReadyAtMs != null &&
    screenshotStats &&
    isMeaningfulMinecraftScene(screenshotStats) &&
    keyboardPass &&
    mousePass
  );
  let archivePath = null;
  if (screenshotStats && fs.existsSync(screenshotPath)) {
    archivePath = path.join(OUT_DIR, `render-canvas-${new Date().toISOString().replace(/[:.]/g, '-')}.png`);
    fs.copyFileSync(screenshotPath, archivePath);
  }
  const report = {
    schemaVersion: 1,
    startedAt,
    finishedAt: new Date().toISOString(),
    root: ROOT,
    url,
    waitMs: WAIT_MS,
    pass,
    worldReadyAtMs,
    sceneReadyAtMs,
    firstNonBlackAtMs,
    firstMeaningfulSceneAtMs,
    latestState: finalState,
    screenshot: {
      latestPath: screenshotPath,
      archivePath,
      stats: screenshotStats
    },
    keyboardSmoke,
    mouseSmoke,
    beacons: beacons.slice(-20),
    console: {
      worldloadSuccess: consoleEvents.filter((line) => line.includes('[mc-web/worldload] probe success:')),
      errors: consoleEvents.filter((line) => line.includes('[ERROR]') || line.includes('[mc-web/gl]')),
      tail: consoleEvents.slice(-120)
    },
    runError,
    pageErrors,
    requestFailures
  };
  const reportPath = path.join(OUT_DIR, 'render-canvas-latest-report.json');
  const archiveReportPath = path.join(OUT_DIR, `render-canvas-${new Date().toISOString().replace(/[:.]/g, '-')}.json`);
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  fs.writeFileSync(archiveReportPath, JSON.stringify(report, null, 2));
  console.log(`render.pass ${pass}`);
  console.log(`render.worldReadyAtMs ${worldReadyAtMs}`);
  console.log(`render.nonBlackRatio ${screenshotStats ? screenshotStats.nonBlackRatio.toFixed(4) : 'null'}`);
  console.log(`render.colorBuckets ${screenshotStats ? screenshotStats.quantizedColorBuckets : 'null'}`);
  console.log(`render.dominantBucketRatio ${screenshotStats ? screenshotStats.dominantBucketRatio.toFixed(4) : 'null'}`);
  console.log(`render.edgeRatio ${screenshotStats ? screenshotStats.edgeRatio.toFixed(4) : 'null'}`);
  console.log(`render.averageRgb ${screenshotStats ? screenshotStats.averageRgb.join(',') : 'null'}`);
  if (keyboardSmoke && keyboardSmoke.enabled) {
    console.log(`keyboard.pass ${keyboardPass}`);
    console.log(`keyboard.keys ${KEYBOARD_KEYS.join(',')}`);
    console.log(`keyboard.errorDisplay ${keyboardSmoke.after ? keyboardSmoke.after.errorDisplay : 'null'}`);
    console.log(`keyboard.pageErrors ${pageErrors.length}`);
    if (keyboardSmoke.after && keyboardSmoke.after.errorText) {
      console.log(`keyboard.errorText ${keyboardSmoke.after.errorText.split(/\r?\n/).slice(0, 8).join(' | ')}`);
    }
  }
  if (mouseSmoke && mouseSmoke.enabled) {
    console.log(`mouse.pass ${mousePass}`);
    console.log(`mouse.canvasFocused ${mouseSmoke.after ? mouseSmoke.after.canvasFocused : 'null'}`);
    console.log(`mouse.pointerLockGranted ${mouseSmoke.after ? mouseSmoke.after.pointerLockGranted : 'null'}`);
    console.log(`mouse.cursorMoved ${mouseSmoke.after ? mouseSmoke.after.cursorMoved : 'null'}`);
    console.log(`mouse.cursorMode ${mouseSmoke.after ? mouseSmoke.after.cursorMode : 'null'}`);
    console.log(`mouse.errorDisplay ${mouseSmoke.after ? mouseSmoke.after.errorDisplay : 'null'}`);
    console.log(`mouse.pageErrors ${pageErrors.length}`);
    if (mouseSmoke.after && mouseSmoke.after.errorText) {
      console.log(`mouse.errorText ${mouseSmoke.after.errorText.split(/\r?\n/).slice(0, 8).join(' | ')}`);
    }
  }
  console.log(`render.screenshot ${screenshotPath}`);
  console.log(`render.report ${reportPath}`);
  if (!pass) process.exitCode = 1;
})().catch((err) => {
  console.error(err && err.stack || String(err));
  process.exitCode = 1;
});
