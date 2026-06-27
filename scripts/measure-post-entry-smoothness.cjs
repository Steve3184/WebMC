const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROOT = path.join(process.cwd(), 'work', 'build', 'web-run');
const OUT_DIR = path.join(process.cwd(), 'output', 'playwright');
const WAIT_MS = Number(process.env.WAIT_MS || 700000);
const SAMPLE_MS = Number(process.env.SAMPLE_MS || 30000);
const READ_STATE_TIMEOUT_MS = Number(process.env.READ_STATE_TIMEOUT_MS || 2500);
const VIEWPORT_WIDTH = Number(process.env.VIEWPORT_WIDTH || 1280);
const VIEWPORT_HEIGHT = Number(process.env.VIEWPORT_HEIGHT || 720);
const WORLD_NAME = String(process.env.WEBMC_WORLD_NAME || 'Web World');
const WEBMC_DIAGNOSTICS = !/^(0|false|no|off)$/i.test(String(process.env.WEBMC_DIAGNOSTICS || '0'));
const WEBMC_FRAMEPROBE = !/^(0|false|no|off)$/i.test(String(process.env.WEBMC_FRAMEPROBE || '1'));

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

function safeJoin(root, reqUrl) {
  const parsed = new URL(reqUrl || '/', 'http://localhost');
  let pathname = decodeURIComponent(parsed.pathname || '/');
  if (pathname === '/' || pathname === '') pathname = '/index.html';
  const abs = path.normalize(path.join(root, pathname));
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
        receivedAt: Date.now(),
        source: parsed.searchParams.get('source') || '',
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
      res.writeHead(200, {
        'Content-Type': MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
        'Cache-Control': 'no-cache'
      });
      res.end(data);
    });
  });
}

function latestBeaconState(beacons) {
  for (let i = beacons.length - 1; i >= 0; i--) {
    if (beacons[i] && beacons[i].state) return beacons[i].state;
  }
  return null;
}

async function evaluateWithTimeout(page, fn, timeoutMs = READ_STATE_TIMEOUT_MS) {
  const timeout = new Promise((resolve) => {
    setTimeout(() => resolve({ __timeout: true }), timeoutMs);
  });
  return Promise.race([page.evaluate(fn).catch((err) => ({ __error: String(err && err.stack || err) })), timeout]);
}

async function readRuntime(page, beacons) {
  const value = await evaluateWithTimeout(page, () => {
    const clone = (value) => {
      try {
        return value == null ? null : JSON.parse(JSON.stringify(value));
      } catch (err) {
        return { cloneError: String(err) };
      }
    };
    return {
      webState: clone(window.__webmcState || null),
      frameProbe: clone(window.__webmcFrameProbe || null),
      tickProbe: clone(window.__webmcTickProbe || null),
      rafCount: Number(window.__webmcRafCount || 0),
      href: location.href
    };
  });
  if (!value || value.__timeout || value.__error) {
    return {
      webState: latestBeaconState(beacons),
      frameProbe: null,
      tickProbe: null,
      rafCount: null,
      readProblem: value || null
    };
  }
  return value;
}

function isReady(state) {
  return !!(
    state &&
    state.levelPresent &&
    state.playerPresent &&
    state.renderWorld &&
    state.worldRenderEligible &&
    state.webTerrainReady &&
    state.screen === 'null' &&
    state.overlay === 'null'
  );
}

function deltaRate(after, before, key, sampleMs) {
  const a = Number(after && after[key] || 0);
  const b = Number(before && before[key] || 0);
  return {
    before: b,
    after: a,
    delta: a - b,
    rate: sampleMs > 0 ? (a - b) * 1000 / sampleMs : 0
  };
}

function summarizeFrameProbe(probe) {
  if (!probe) return null;
  const count = Number(probe.count || 0);
  const recent = Array.isArray(probe.recent) ? probe.recent : [];
  const totals = probe.totals || {};
  const averages = {};
  for (const [key, value] of Object.entries(totals)) {
    averages[key] = count > 0 ? Number(value || 0) / count : 0;
  }
  return {
    count,
    fpsString: probe.fpsString || '',
    frameCount: probe.frameCount || 0,
    clientTickCount: probe.clientTickCount || 0,
    averages,
    max: probe.max || {},
    last: probe.last || null,
    recentTail: recent.slice(-12)
  };
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
  const url = `http://127.0.0.1:${port}/?boot=mcMain&autostart=1&world=${encodeURIComponent(WORLD_NAME)}${WEBMC_DIAGNOSTICS ? '&diagnostics=1' : ''}${WEBMC_FRAMEPROBE ? '&frameprobe=1' : ''}&t=${Date.now()}`;

  let browser = null;
  let result = null;
  try {
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT } });
    await page.addInitScript(({ diagnostics, frameProbe }) => {
      window.webmcBootMode = 'mcMain';
      window.webmcDiagnostics = !!diagnostics;
      window.webmcFrameProbe = !!frameProbe;
      window.__webmcRafCount = 0;
      const loop = () => {
        window.__webmcRafCount = (window.__webmcRafCount || 0) + 1;
        requestAnimationFrame(loop);
      };
      requestAnimationFrame(loop);
    }, { diagnostics: WEBMC_DIAGNOSTICS, frameProbe: WEBMC_FRAMEPROBE });

    page.on('console', (msg) => {
      consoleEvents.push({ type: msg.type(), text: msg.text(), t: Date.now() });
    });
    page.on('pageerror', (err) => pageErrors.push(String(err && err.stack || err)));
    page.on('requestfailed', (req) => requestFailures.push({ url: req.url(), error: req.failure() && req.failure().errorText }));

    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 90000 });
    const start = Date.now();
    let readyAtMs = null;
    let readyRuntime = null;
    while (Date.now() - start < WAIT_MS) {
      const runtime = await readRuntime(page, beacons);
      const state = runtime.webState;
      if (isReady(state)) {
        readyAtMs = Date.now() - start;
        readyRuntime = runtime;
        break;
      }
      await page.waitForTimeout(1000);
    }

    if (readyAtMs == null) {
      const runtime = await readRuntime(page, beacons);
      throw new Error(`Timed out waiting for playable world. latest=${JSON.stringify(runtime.webState || null)}`);
    }

    const before = await readRuntime(page, beacons);
    await page.waitForTimeout(SAMPLE_MS);
    const after = await readRuntime(page, beacons);

    const screenshot = path.join(OUT_DIR, 'post-entry-smoothness-latest.png');
    await page.screenshot({ path: screenshot, fullPage: true, timeout: 30000 }).catch((err) => {
      consoleEvents.push({ type: 'screenshot-error', text: String(err && err.stack || err), t: Date.now() });
    });

    const beforeState = before.webState || {};
    const afterState = after.webState || {};
    const metrics = {
      sampleMs: SAMPLE_MS,
      raf: {
        before: Number(before.rafCount || 0),
        after: Number(after.rafCount || 0),
        delta: Number(after.rafCount || 0) - Number(before.rafCount || 0),
        rate: (Number(after.rafCount || 0) - Number(before.rafCount || 0)) * 1000 / SAMPLE_MS
      },
      mcFrame: deltaRate(afterState, beforeState, 'mcFrameCount', SAMPLE_MS),
      present: deltaRate(afterState, beforeState, 'presentCount', SAMPLE_MS),
      renderGate: deltaRate(afterState, beforeState, 'renderGateUpdates', SAMPLE_MS),
      levelRender: deltaRate(afterState, beforeState, 'levelRenderUpdates', SAMPLE_MS),
      clientTick: deltaRate(afterState, beforeState, 'clientTickCount', SAMPLE_MS),
      frameProbe: deltaRate(afterState, beforeState, 'frameProbeUpdates', SAMPLE_MS),
      tickProbe: deltaRate(afterState, beforeState, 'tickProbeUpdates', SAMPLE_MS)
    };

    result = {
      ok: metrics.present.rate >= 10 && metrics.mcFrame.rate >= 10,
      url,
      diagnostics: WEBMC_DIAGNOSTICS,
      frameProbeEnabled: WEBMC_FRAMEPROBE,
      readyAtMs,
      readyState: readyRuntime && readyRuntime.webState,
      beforeState,
      afterState,
      metrics,
      frameProbe: summarizeFrameProbe(after.frameProbe),
      tickProbe: summarizeFrameProbe(after.tickProbe),
      pageErrors,
      requestFailures,
      uboResizeLogCount: consoleEvents.filter((event) => event.text.includes('Resizing Dynamic Transforms UBO')).length,
      cantKeepUpCount: consoleEvents.filter((event) => event.text.includes("Can't keep up")).length,
      gpuStallReadPixelsCount: consoleEvents.filter((event) => event.text.includes('GPU stall due to ReadPixels')).length,
      consoleTail: consoleEvents.slice(-100),
      screenshot
    };
  } catch (err) {
    result = {
      ok: false,
      error: String(err && err.stack || err),
      pageErrors,
      requestFailures,
      beaconsTail: beacons.slice(-12),
      consoleTail: consoleEvents.slice(-100)
    };
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close().catch(() => {});
    await new Promise((resolve) => server.close(resolve)).catch(() => {});
    const report = path.join(OUT_DIR, 'post-entry-smoothness-latest.json');
    fs.writeFileSync(report, JSON.stringify(result, null, 2));
    console.log(`smoothness.report=${report}`);
    if (result && result.metrics) {
      console.log(`smoothness.readyAtMs=${result.readyAtMs}`);
      console.log(`smoothness.rafRate=${result.metrics.raf.rate.toFixed(2)}`);
      console.log(`smoothness.mcFrameRate=${result.metrics.mcFrame.rate.toFixed(2)}`);
      console.log(`smoothness.presentRate=${result.metrics.present.rate.toFixed(2)}`);
      console.log(`smoothness.clientTickRate=${result.metrics.clientTick.rate.toFixed(2)}`);
      if (result.frameProbe && result.frameProbe.last) {
        console.log(`smoothness.frameProbeLast=${JSON.stringify(result.frameProbe.last.stages)}`);
      }
      if (result.tickProbe && result.tickProbe.last) {
        console.log(`smoothness.tickProbeLast=${JSON.stringify(result.tickProbe.last.stages)}`);
      }
    }
  }
})();
