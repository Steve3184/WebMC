const http = require('http');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { chromium } = require('playwright');

const ROOT = path.join(process.cwd(), 'work', 'build', 'web-run');
const OUT_DIR = path.join(process.cwd(), 'output', 'playwright');
const WAIT_MS = Number(process.env.WAIT_MS || 430000);
const VIEWPORT_WIDTH = Number(process.env.VIEWPORT_WIDTH || 1280);
const VIEWPORT_HEIGHT = Number(process.env.VIEWPORT_HEIGHT || 720);
const WORLD_NAME = String(process.env.WEBMC_WORLD_NAME || 'Web World');
const AFTER_READY_SAMPLE_MS = Number(process.env.AFTER_READY_SAMPLE_MS || 3000);
const STATE_DRAW_SAMPLE_LIMIT = Number(process.env.STATE_DRAW_SAMPLE_LIMIT || 12);
const DEEP_DRAW_SAMPLE_LIMIT = Number(process.env.DEEP_DRAW_SAMPLE_LIMIT || 2);

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
      const raw = parsed.searchParams.get('d');
      let state = null;
      if (raw) {
        try {
          state = JSON.parse(raw);
        } catch {
          state = { parseError: true, raw };
        }
      }
      beacons.push({ source: parsed.searchParams.get('source'), state, receivedAt: new Date().toISOString() });
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
        value = pa <= pb && pa <= pc ? left : pb <= pc ? up : upLeft;
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
  const chunks = readChunks(png);
  const ihdr = chunks.find((c) => c.type === 'IHDR');
  if (!ihdr) throw new Error('Missing IHDR');
  const width = ihdr.data.readUInt32BE(0);
  const height = ihdr.data.readUInt32BE(4);
  const colorType = ihdr.data[9];
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
  const buckets = new Map();
  for (let i = 0; i < total; i += step) {
    const p = i * bpp;
    const r = pixels[p];
    const g = pixels[p + 1];
    const b = pixels[p + 2];
    sampled++;
    nonBlack += r + g + b > 12 ? 1 : 0;
    rSum += r;
    gSum += g;
    bSum += b;
    r2Sum += r * r;
    g2Sum += g * g;
    b2Sum += b * b;
    const bucket = `${r >> 4}:${g >> 4}:${b >> 4}`;
    buckets.set(bucket, (buckets.get(bucket) || 0) + 1);
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
      edgeHits += dr > 36 ? 1 : 0;
      edgeHits += dd > 36 ? 1 : 0;
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
    averageRgb: [Math.round(avgR), Math.round(avgG), Math.round(avgB)],
    varianceRgb: varianceRgb.map((v) => Math.round(v)),
    quantizedColorBuckets: buckets.size,
    dominantBucketRatio: Math.max(...buckets.values()) / sampled,
    edgeRatio: edgeSamples ? edgeHits / edgeSamples : 0,
    topBuckets: Array.from(buckets.entries()).sort((a, b) => b[1] - a[1]).slice(0, 8)
  };
}

async function writeCanvasPngFromPage(page, filePath) {
  const dataUrl = await page.evaluate(() => {
    const canvas = document.querySelector('#canvas, canvas');
    if (!canvas || typeof canvas.toDataURL !== 'function') return null;
    return canvas.toDataURL('image/png');
  });
  if (!dataUrl || !dataUrl.startsWith('data:image/png;base64,')) {
    throw new Error('Canvas toDataURL did not return a PNG');
  }
  fs.writeFileSync(filePath, Buffer.from(dataUrl.slice('data:image/png;base64,'.length), 'base64'));
}

function latestState(beacons) {
  for (let i = beacons.length - 1; i >= 0; i--) {
    if (beacons[i].state) return beacons[i].state;
  }
  return null;
}

async function readLatestState(page, beacons) {
  const pageState = page
    ? await page.evaluate(() => {
      const holder = globalThis.__webmcLatestState;
      return holder && holder.state ? holder.state : null;
    }).catch(() => null)
    : null;
  return pageState || latestState(beacons);
}

function sceneReady(state, consoleEvents) {
  return !!(
    consoleEvents.some((line) => line.includes('[mc-web/worldload] probe success:')) ||
    (state && state.gameLoadFinished && state.levelPresent && state.playerPresent && state.worldRenderEligible && Number(state.renderedSections || 0) > 0)
  );
}

async function main() {
  if (!fs.existsSync(path.join(ROOT, 'index.html'))) {
    throw new Error(`Missing web-run build at ${ROOT}`);
  }
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const beacons = [];
  const consoleEvents = [];
  const server = createServer(beacons);
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}/?boot=mcMain&autostart=1&world=${encodeURIComponent(WORLD_NAME)}&t=${Date.now()}`;
  let browser = null;
  let context = null;
  let page = null;
  let report = null;
  try {
  browser = await chromium.launch({ headless: true });
  context = await browser.newContext({ viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT } });
  await context.addInitScript(({ stateDrawSampleLimit, deepDrawSampleLimit }) => {
    const install = () => {
      const Ctor = globalThis.WebGL2RenderingContext;
      if (!Ctor || Ctor.prototype.__webmcDrawProbeInstalled) return;
      const proto = Ctor.prototype;
      const state = globalThis.__webmcDrawProbe = {
        installedAt: Date.now(),
        drawElements: 0,
        drawElementsInstanced: 0,
        drawArrays: 0,
        clear: 0,
        blitFramebuffer: 0,
        useProgram: 0,
        drawSamples: [],
        clearSamples: [],
        drawStateSamples: [],
        framebufferSamples: [],
        programDetails: [],
        programs: [],
        errors: []
      };
      const programIds = new WeakMap();
      let nextProgramId = 1;
      let currentProgramId = 0;
      let currentProgramObj = null;
      const getProgramId = (program) => {
        if (!program) return 0;
        if (!programIds.has(program)) programIds.set(program, nextProgramId++);
        return programIds.get(program);
      };
      const bufferIds = new WeakMap();
      const bufferObjects = {};
      const bufferContents = {};
      let nextBufferId = 1;
      const getBufferId = (buffer) => {
        if (!buffer) return 0;
        if (!bufferIds.has(buffer)) {
          const id = nextBufferId++;
          bufferIds.set(buffer, id);
          bufferObjects[id] = buffer;
        }
        return bufferIds.get(buffer);
      };
      const textureIds = new WeakMap();
      let nextTextureId = 1;
      const getTextureId = (texture) => {
        if (!texture) return 0;
        if (!textureIds.has(texture)) textureIds.set(texture, nextTextureId++);
        return textureIds.get(texture);
      };
      const tracked = {
        arrayBuffer: 0,
        elementArrayBuffer: 0,
        uniformBuffer: 0,
        activeTextureUnit: 0,
        textureBindings: {},
        uniformBuffers: {},
        attribPointers: {}
      };
      const currentBufferIdForTarget = (gl, target) => {
        if (target === gl.ARRAY_BUFFER) return tracked.arrayBuffer;
        if (target === gl.ELEMENT_ARRAY_BUFFER) return tracked.elementArrayBuffer;
        if (target === gl.UNIFORM_BUFFER) return tracked.uniformBuffer;
        return 0;
      };
      const bytesFromSource = (src, srcOffset = 0, length = undefined) => {
        if (typeof src === 'number') return new Uint8Array(Math.max(0, src));
        if (!src) return null;
        let bytes = null;
        let bytesPerElement = 1;
        let elementLength = 0;
        if (src instanceof ArrayBuffer) {
          bytes = new Uint8Array(src);
          elementLength = bytes.byteLength;
        } else if (ArrayBuffer.isView(src)) {
          bytes = new Uint8Array(src.buffer, src.byteOffset, src.byteLength);
          bytesPerElement = src.BYTES_PER_ELEMENT || 1;
          elementLength = typeof src.length === 'number' ? src.length : src.byteLength;
        }
        if (!bytes) return null;
        const start = Math.max(0, Number(srcOffset || 0) * bytesPerElement);
        const count = length == null ? (elementLength - Number(srcOffset || 0)) : Number(length || 0);
        const byteLength = Math.max(0, count * bytesPerElement);
        return new Uint8Array(bytes.slice(start, Math.min(bytes.byteLength, start + byteLength)));
      };
      const replaceBufferContents = (id, bytes) => {
        if (!id || !bytes) return;
        bufferContents[id] = new Uint8Array(bytes);
      };
      const patchBufferContents = (id, offset, bytes) => {
        if (!id || !bytes) return;
        const dstOffset = Math.max(0, Number(offset || 0));
        const oldBytes = bufferContents[id] || new Uint8Array(0);
        const next = new Uint8Array(Math.max(oldBytes.byteLength, dstOffset + bytes.byteLength));
        next.set(oldBytes, 0);
        next.set(bytes, dstOffset);
        bufferContents[id] = next;
      };
      const bufferSlice = (id, offset, length) => {
        const bytes = bufferContents[id];
        if (!bytes) return null;
        const start = Math.max(0, Number(offset || 0));
        const end = Math.min(bytes.byteLength, start + Math.max(0, Number(length || 0)));
        return bytes.slice(start, end);
      };
      const firstFloats = (bytes, maxCount) => {
        if (!bytes) return [];
        const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
        const count = Math.min(maxCount, Math.floor(bytes.byteLength / 4));
        const out = [];
        for (let i = 0; i < count; i++) out.push(+view.getFloat32(i * 4, true).toFixed(5));
        return out;
      };
      const mat4MulVecColumnMajor = (m, v) => {
        if (!m || m.length < 16) return null;
        const x = v[0], y = v[1], z = v[2], w = v[3];
        return [
          m[0] * x + m[4] * y + m[8] * z + m[12] * w,
          m[1] * x + m[5] * y + m[9] * z + m[13] * w,
          m[2] * x + m[6] * y + m[10] * z + m[14] * w,
          m[3] * x + m[7] * y + m[11] * z + m[15] * w
        ];
      };
      const mat4MulVecRowMajor = (m, v) => {
        if (!m || m.length < 16) return null;
        const x = v[0], y = v[1], z = v[2], w = v[3];
        return [
          m[0] * x + m[1] * y + m[2] * z + m[3] * w,
          m[4] * x + m[5] * y + m[6] * z + m[7] * w,
          m[8] * x + m[9] * y + m[10] * z + m[11] * w,
          m[12] * x + m[13] * y + m[14] * z + m[15] * w
        ];
      };
      const drainErrors = (gl, limit = 8) => {
        const errors = [];
        try {
          for (let i = 0; i < limit; i++) {
            const err = gl.getError();
            if (!err) break;
            errors.push(err);
          }
        } catch (_) {}
        return errors;
      };
      const sampleDraw = (kind, args) => {
        if (state.drawSamples.length < 240) {
          state.drawSamples.push({
            kind,
            program: currentProgramId,
            args: Array.from(args).slice(0, 8),
            t: Date.now() - state.installedAt
          });
        }
      };
      const shaderIds = new WeakMap();
      const shaderSources = new WeakMap();
      const programShaders = new WeakMap();
      let nextShaderId = 1;
      const getShaderId = (shader) => {
        if (!shader) return 0;
        if (!shaderIds.has(shader)) shaderIds.set(shader, nextShaderId++);
        return shaderIds.get(shader);
      };
      const oldDrawElements = proto.drawElements;
      const oldDrawElementsInstanced = proto.drawElementsInstanced;
      const oldDrawArrays = proto.drawArrays;
      const oldClear = proto.clear;
      const oldBlit = proto.blitFramebuffer;
      const oldCreateBuffer = proto.createBuffer;
      const oldBindBuffer = proto.bindBuffer;
      const oldBufferData = proto.bufferData;
      const oldBufferSubData = proto.bufferSubData;
      const oldBindBufferBase = proto.bindBufferBase;
      const oldBindBufferRange = proto.bindBufferRange;
      const oldCreateTexture = proto.createTexture;
      const oldActiveTexture = proto.activeTexture;
      const oldBindTexture = proto.bindTexture;
      const oldVertexAttribPointer = proto.vertexAttribPointer;
      const oldVertexAttribIPointer = proto.vertexAttribIPointer;
      const oldEnableVertexAttribArray = proto.enableVertexAttribArray;
      const oldDisableVertexAttribArray = proto.disableVertexAttribArray;
      const oldBindFramebuffer = proto.bindFramebuffer;
      const oldCreateProgram = proto.createProgram;
      const oldCreateShader = proto.createShader;
      const oldShaderSource = proto.shaderSource;
      const oldAttachShader = proto.attachShader;
      const oldLinkProgram = proto.linkProgram;
      const oldUseProgram = proto.useProgram;
      let boundFramebuffer = 0;
      let nextFramebufferId = 1;
      const framebufferIds = new WeakMap();
      const getFramebufferId = (fb) => {
        if (!fb) return 0;
        if (!framebufferIds.has(fb)) framebufferIds.set(fb, nextFramebufferId++);
        return framebufferIds.get(fb);
      };
      const summarizeProgram = (gl, program) => {
        if (!program) return null;
        const shaders = programShaders.get(program) || [];
        const attribs = [];
        const uniforms = [];
        const uniformBlocks = [];
        try {
          const n = gl.getProgramParameter(program, gl.ACTIVE_ATTRIBUTES) || 0;
          for (let i = 0; i < n; i++) {
            const info = gl.getActiveAttrib(program, i);
            if (!info) continue;
            attribs.push({
              name: info.name,
              size: info.size,
              type: info.type,
              location: gl.getAttribLocation(program, info.name)
            });
          }
        } catch (e) {
          attribs.push({ error: String(e && e.message || e) });
        }
        try {
          const n = gl.getProgramParameter(program, gl.ACTIVE_UNIFORMS) || 0;
          for (let i = 0; i < n; i++) {
            const info = gl.getActiveUniform(program, i);
            if (!info) continue;
            const isSampler = [
              gl.SAMPLER_2D,
              gl.SAMPLER_3D,
              gl.SAMPLER_CUBE,
              gl.SAMPLER_2D_ARRAY,
              gl.INT_SAMPLER_2D,
              gl.UNSIGNED_INT_SAMPLER_2D,
              gl.SAMPLER_2D_SHADOW
            ].includes(info.type);
            uniforms.push({
              name: info.name,
              size: info.size,
              type: info.type,
              sampler: isSampler
            });
          }
        } catch (e) {
          uniforms.push({ error: String(e && e.message || e) });
        }
        try {
          const n = gl.getProgramParameter(program, gl.ACTIVE_UNIFORM_BLOCKS) || 0;
          for (let i = 0; i < n; i++) {
            uniformBlocks.push({
              index: i,
              name: gl.getActiveUniformBlockName(program, i),
              binding: gl.getActiveUniformBlockParameter(program, i, gl.UNIFORM_BLOCK_BINDING),
              dataSize: gl.getActiveUniformBlockParameter(program, i, gl.UNIFORM_BLOCK_DATA_SIZE)
            });
          }
        } catch (e) {
          uniformBlocks.push({ error: String(e && e.message || e) });
        }
        return {
          id: getProgramId(program),
          attribs,
          uniforms,
          uniformBlocks,
          shaders: shaders.map((shader) => {
            const info = shaderSources.get(shader) || {};
            const src = info.source || '';
            return {
              shader: getShaderId(shader),
              type: info.type,
              length: src.length,
              terrain: src.indexOf('ModelOffset') >= 0 && src.indexOf('Sampler2') >= 0 && src.indexOf('vertexColor') >= 0,
              snippet: src.slice(0, 180)
            };
          })
        };
      };
      const programDetailsById = {};
      const captureUniformBlocks = (gl) => {
        const blocks = [];
        if (!currentProgramObj) return blocks;
        try {
          const n = gl.getProgramParameter(currentProgramObj, gl.ACTIVE_UNIFORM_BLOCKS) || 0;
          for (let i = 0; i < n; i++) {
            const name = gl.getActiveUniformBlockName(currentProgramObj, i);
            const binding = gl.getActiveUniformBlockParameter(currentProgramObj, i, gl.UNIFORM_BLOCK_BINDING);
            const dataSize = gl.getActiveUniformBlockParameter(currentProgramObj, i, gl.UNIFORM_BLOCK_DATA_SIZE);
            const bound = tracked.uniformBuffers[binding] || null;
            const offset = bound && bound.offset ? Number(bound.offset) : 0;
            const rangeSize = bound && bound.size ? Number(bound.size) : (bound && bufferContents[bound.buffer] ? bufferContents[bound.buffer].byteLength : 0);
            const readSize = Math.min(rangeSize || dataSize || 0, Math.max(0, dataSize || 0), 320) || Math.min(rangeSize || 0, 320);
            const bytes = bound ? bufferSlice(bound.buffer, offset, readSize) : null;
            const floats = firstFloats(bytes, 80);
            let decoded = null;
            if (name === 'DynamicTransforms' && floats.length >= 41) {
              decoded = {
                modelView: floats.slice(0, 16),
                colorModulator: floats.slice(16, 20),
                modelOffset: floats.slice(20, 23),
                textureMatrix: floats.slice(24, 40),
                lineWidth: floats[40]
              };
            } else if (name === 'Projection' && floats.length >= 16) {
              decoded = { projMat: floats.slice(0, 16) };
            } else if (name === 'Fog' && floats.length) {
              decoded = { firstFloats: floats.slice(0, 16) };
            } else if (name === 'Globals' && floats.length) {
              decoded = { firstFloats: floats.slice(0, 16) };
            }
            blocks.push({
              index: i,
              name,
              binding,
              dataSize,
              bound,
              capturedBytes: bytes ? bytes.byteLength : 0,
              firstFloats: floats.slice(0, 24),
              decoded
            });
          }
        } catch (e) {
          blocks.push({ error: String(e && e.message || e) });
        }
        return blocks;
      };
      const captureDrawInputs = (gl, args, attribs, uniformBlocks) => {
        try {
          const argsArray = Array.from(args).slice(0, 8);
          const count = Number(argsArray[1] || 0);
          const indexType = Number(argsArray[2] || 0);
          const indexOffset = Number(argsArray[3] || 0);
          const positionAttrib = attribs.find((a) => a && a.name === 'Position' && typeof a.offset === 'number');
          const colorAttrib = attribs.find((a) => a && a.name === 'Color' && typeof a.offset === 'number');
          const uv0Attrib = attribs.find((a) => a && a.name === 'UV0' && typeof a.offset === 'number');
          const uv2Attrib = attribs.find((a) => a && a.name === 'UV2' && typeof a.offset === 'number');
          const vertexBuffer = positionAttrib ? positionAttrib.buffer : tracked.arrayBuffer;
          const indexBuffer = tracked.elementArrayBuffer;
          const vBytes = bufferContents[vertexBuffer] || null;
          const iBytes = bufferContents[indexBuffer] || null;
          const vView = vBytes ? new DataView(vBytes.buffer, vBytes.byteOffset, vBytes.byteLength) : null;
          const iView = iBytes ? new DataView(iBytes.buffer, iBytes.byteOffset, iBytes.byteLength) : null;
          const indexByteSize = indexType === gl.UNSIGNED_INT ? 4 : 2;
          const indices = [];
          const indexLimit = Math.min(48, count);
          if (iView) {
            for (let i = 0; i < indexLimit; i++) {
              const off = indexOffset + i * indexByteSize;
              if (off + indexByteSize > iView.byteLength) break;
              indices.push(indexByteSize === 4 ? iView.getUint32(off, true) : iView.getUint16(off, true));
            }
          }
          const readVertex = (idx) => {
            if (!vView || !positionAttrib) return null;
            const stride = Number(positionAttrib.stride || 0);
            const base = idx * stride;
            const posOff = base + Number(positionAttrib.offset || 0);
            if (stride <= 0 || posOff + 12 > vView.byteLength) return null;
            const vertex = {
              index: idx,
              position: [
                +vView.getFloat32(posOff, true).toFixed(5),
                +vView.getFloat32(posOff + 4, true).toFixed(5),
                +vView.getFloat32(posOff + 8, true).toFixed(5)
              ]
            };
            if (colorAttrib) {
              const off = base + Number(colorAttrib.offset || 0);
              if (off + 4 <= vView.byteLength) {
                vertex.color = [vView.getUint8(off), vView.getUint8(off + 1), vView.getUint8(off + 2), vView.getUint8(off + 3)];
              }
            }
            if (uv0Attrib) {
              const off = base + Number(uv0Attrib.offset || 0);
              if (off + 8 <= vView.byteLength) {
                vertex.uv0 = [+vView.getFloat32(off, true).toFixed(5), +vView.getFloat32(off + 4, true).toFixed(5)];
              }
            }
            if (uv2Attrib) {
              const off = base + Number(uv2Attrib.offset || 0);
              if (off + 4 <= vView.byteLength) {
                vertex.uv2 = [vView.getInt16(off, true), vView.getInt16(off + 2, true)];
              }
            }
            return vertex;
          };
          const seen = new Set();
          const vertices = [];
          for (const idx of indices) {
            if (seen.has(idx)) continue;
            seen.add(idx);
            const v = readVertex(idx);
            if (v) vertices.push(v);
            if (vertices.length >= 16) break;
          }
          const dynamic = (uniformBlocks || []).find((b) => b.name === 'DynamicTransforms' && b.decoded);
          const projection = (uniformBlocks || []).find((b) => b.name === 'Projection' && b.decoded);
          const modelView = dynamic && dynamic.decoded ? dynamic.decoded.modelView : null;
          const modelOffset = dynamic && dynamic.decoded ? dynamic.decoded.modelOffset : [0, 0, 0];
          const projMat = projection && projection.decoded ? projection.decoded.projMat : null;
          const makeClipStats = (mulFn) => {
            if (!modelView || !projMat || !vertices.length) return null;
            let inside = 0;
            let finite = 0;
            const bbox = { minX: Infinity, minY: Infinity, minZ: Infinity, maxX: -Infinity, maxY: -Infinity, maxZ: -Infinity };
            const samples = [];
            for (const v of vertices.slice(0, 12)) {
              const p = v.position;
              const local = [p[0] + (modelOffset[0] || 0), p[1] + (modelOffset[1] || 0), p[2] + (modelOffset[2] || 0), 1];
              const eye = mulFn(modelView, local);
              const clip = eye ? mulFn(projMat, eye) : null;
              if (!clip || !clip.every(Number.isFinite)) continue;
              finite++;
              const w = clip[3];
              const ndc = w ? [clip[0] / w, clip[1] / w, clip[2] / w] : [Infinity, Infinity, Infinity];
              const inClip = w > 0 && Math.abs(clip[0]) <= w && Math.abs(clip[1]) <= w && clip[2] >= -w && clip[2] <= w;
              if (inClip) inside++;
              if (ndc.every(Number.isFinite)) {
                bbox.minX = Math.min(bbox.minX, ndc[0]); bbox.maxX = Math.max(bbox.maxX, ndc[0]);
                bbox.minY = Math.min(bbox.minY, ndc[1]); bbox.maxY = Math.max(bbox.maxY, ndc[1]);
                bbox.minZ = Math.min(bbox.minZ, ndc[2]); bbox.maxZ = Math.max(bbox.maxZ, ndc[2]);
              }
              if (samples.length < 6) {
                samples.push({
                  index: v.index,
                  position: v.position,
                  eye: eye.map((n) => +n.toFixed(4)),
                  clip: clip.map((n) => +n.toFixed(4)),
                  ndc: ndc.map((n) => Number.isFinite(n) ? +n.toFixed(4) : n),
                  inClip
                });
              }
            }
            return {
              probed: vertices.length,
              finite,
              inside,
              bboxNdc: Number.isFinite(bbox.minX) ? {
                min: [+bbox.minX.toFixed(4), +bbox.minY.toFixed(4), +bbox.minZ.toFixed(4)],
                max: [+bbox.maxX.toFixed(4), +bbox.maxY.toFixed(4), +bbox.maxZ.toFixed(4)]
              } : null,
              samples
            };
          };
          return {
            vertexBuffer: { id: vertexBuffer, byteLength: vBytes ? vBytes.byteLength : 0 },
            indexBuffer: { id: indexBuffer, byteLength: iBytes ? iBytes.byteLength : 0, indexType, indexOffset },
            indices,
            vertices,
            clipColumnMajor: makeClipStats(mat4MulVecColumnMajor),
            clipRowMajor: makeClipStats(mat4MulVecRowMajor)
          };
        } catch (e) {
          return { error: String(e && e.stack || e) };
        }
      };
      const snapshotDrawState = (gl, tag, args, drawErrors, preErrors) => {
        if (state.drawStateSamples.length >= stateDrawSampleLimit) return;
        const programInfo = programDetailsById[currentProgramId] || null;
        const attribs = [];
        try {
          for (const a of (programInfo && programInfo.attribs) || []) {
            const loc = a.location;
            if (typeof loc !== 'number' || loc < 0) {
              attribs.push(a);
              continue;
            }
            attribs.push({
              name: a.name,
              shaderType: a.type,
              location: loc,
              enabled: !!gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_ENABLED),
              integer: !!gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_INTEGER),
              buffer: getBufferId(gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_BUFFER_BINDING)),
              size: gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_SIZE),
              type: gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_TYPE),
              normalized: !!gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_NORMALIZED),
              stride: gl.getVertexAttrib(loc, gl.VERTEX_ATTRIB_ARRAY_STRIDE),
              offset: gl.getVertexAttribOffset(loc, gl.VERTEX_ATTRIB_ARRAY_POINTER),
              tracked: tracked.attribPointers[loc] || null
            });
          }
        } catch (e) {
          attribs.push({ error: String(e && e.message || e) });
        }
        const samplers = [];
        try {
          if (currentProgramObj && programInfo) {
            for (const u of programInfo.uniforms || []) {
              if (!u.sampler) continue;
              const loc = gl.getUniformLocation(currentProgramObj, u.name);
              const unit = loc ? gl.getUniform(currentProgramObj, loc) : null;
              const binding = unit == null ? null : (tracked.textureBindings[unit] || {});
              samplers.push({
                name: u.name,
                type: u.type,
                unit,
                texture2D: binding ? (binding[gl.TEXTURE_2D] || 0) : 0
              });
            }
          }
        } catch (e) {
          samplers.push({ error: String(e && e.message || e) });
        }
        const getParam = (p) => {
          try {
            const v = gl.getParameter(p);
            return ArrayBuffer.isView(v) ? Array.from(v) : v;
          } catch (e) {
            return 'err:' + String(e && e.message || e);
          }
        };
        const uniformBlocks = captureUniformBlocks(gl);
        const bufferInputs = state.drawStateSamples.length < deepDrawSampleLimit
          ? captureDrawInputs(gl, args, attribs, uniformBlocks)
          : null;
        state.drawStateSamples.push({
          tag,
          program: currentProgramId,
          args: Array.from(args).slice(0, 8),
          drawErrors,
          preErrors,
          framebuffer: boundFramebuffer,
          state: {
            viewport: getParam(gl.VIEWPORT),
            colorMask: getParam(gl.COLOR_WRITEMASK),
            depthMask: getParam(gl.DEPTH_WRITEMASK),
            depthFunc: getParam(gl.DEPTH_FUNC),
            depthTest: !!gl.isEnabled(gl.DEPTH_TEST),
            cullFace: !!gl.isEnabled(gl.CULL_FACE),
            blend: !!gl.isEnabled(gl.BLEND),
            scissor: !!gl.isEnabled(gl.SCISSOR_TEST),
            arrayBuffer: tracked.arrayBuffer,
            elementArrayBuffer: tracked.elementArrayBuffer,
            uniformBuffers: JSON.parse(JSON.stringify(tracked.uniformBuffers))
          },
          attribs,
          samplers,
          uniformBlocks,
          bufferInputs,
          t: Date.now() - state.installedAt
        });
      };
      const sampleFramebuffer = (gl, tag) => {
        if (state.framebufferSamples.length >= 80) return;
        try {
          const preErrors = drainErrors(gl);
          const w = 16, h = 9;
          const pixels = new Uint8Array(w * h * 4);
          gl.readPixels(0, 0, w, h, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
          const readErrors = drainErrors(gl);
          const buckets = {};
          let rSum = 0, gSum = 0, bSum = 0, nonBlack = 0;
          for (let i = 0; i < w * h; i++) {
            const p = i * 4;
            const r = pixels[p], g = pixels[p + 1], b = pixels[p + 2];
            rSum += r; gSum += g; bSum += b;
            if (r + g + b > 12) nonBlack++;
            buckets[(r >> 4) + ':' + (g >> 4) + ':' + (b >> 4)] = 1;
          }
          state.framebufferSamples.push({
            tag,
            framebuffer: boundFramebuffer,
            avg: [Math.round(rSum / (w * h)), Math.round(gSum / (w * h)), Math.round(bSum / (w * h))],
            buckets: Object.keys(buckets).length,
            nonBlackRatio: +(nonBlack / (w * h)).toFixed(3),
            preErrors,
            readErrors,
            t: Date.now() - state.installedAt
          });
        } catch (e) {
          state.framebufferSamples.push({ tag, framebuffer: boundFramebuffer, error: String(e && e.message || e), t: Date.now() - state.installedAt });
        }
      };
      proto.createShader = function(type) {
        const shader = oldCreateShader.apply(this, arguments);
        if (shader) shaderSources.set(shader, { type, source: '' });
        return shader;
      };
      proto.shaderSource = function(shader, source) {
        const info = shaderSources.get(shader) || { type: 0, source: '' };
        info.source = String(source || '');
        shaderSources.set(shader, info);
        return oldShaderSource.apply(this, arguments);
      };
      proto.createProgram = function() {
        const program = oldCreateProgram.apply(this, arguments);
        if (program) programShaders.set(program, []);
        return program;
      };
      proto.attachShader = function(program, shader) {
        if (program && shader) {
          const shaders = programShaders.get(program) || [];
          shaders.push(shader);
          programShaders.set(program, shaders);
        }
        return oldAttachShader.apply(this, arguments);
      };
      proto.linkProgram = function(program) {
        const result = oldLinkProgram.apply(this, arguments);
        const summary = summarizeProgram(this, program);
        if (summary) {
          programDetailsById[summary.id] = summary;
          if (state.programDetails.length < 120) state.programDetails.push(summary);
        }
        return result;
      };
      proto.createBuffer = function() {
        const buffer = oldCreateBuffer.apply(this, arguments);
        if (buffer) getBufferId(buffer);
        return buffer;
      };
      proto.bindBuffer = function(target, buffer) {
        const id = getBufferId(buffer);
        if (target === this.ARRAY_BUFFER) tracked.arrayBuffer = id;
        if (target === this.ELEMENT_ARRAY_BUFFER) tracked.elementArrayBuffer = id;
        if (target === this.UNIFORM_BUFFER) tracked.uniformBuffer = id;
        return oldBindBuffer.apply(this, arguments);
      };
      proto.bufferData = function(target, dataOrSize, usage, srcOffset, length) {
        const result = oldBufferData.apply(this, arguments);
        const id = currentBufferIdForTarget(this, target);
        const bytes = bytesFromSource(dataOrSize, srcOffset, length);
        replaceBufferContents(id, bytes);
        return result;
      };
      proto.bufferSubData = function(target, dstByteOffset, srcData, srcOffset, length) {
        const result = oldBufferSubData.apply(this, arguments);
        const id = currentBufferIdForTarget(this, target);
        const bytes = bytesFromSource(srcData, srcOffset, length);
        patchBufferContents(id, dstByteOffset, bytes);
        return result;
      };
      proto.bindBufferBase = function(target, index, buffer) {
        if (target === this.UNIFORM_BUFFER) tracked.uniformBuffers[index] = { buffer: getBufferId(buffer), base: true };
        return oldBindBufferBase.apply(this, arguments);
      };
      proto.bindBufferRange = function(target, index, buffer, offset, size) {
        if (target === this.UNIFORM_BUFFER) tracked.uniformBuffers[index] = { buffer: getBufferId(buffer), offset, size, base: false };
        return oldBindBufferRange.apply(this, arguments);
      };
      proto.createTexture = function() {
        const texture = oldCreateTexture.apply(this, arguments);
        if (texture) getTextureId(texture);
        return texture;
      };
      proto.activeTexture = function(unit) {
        tracked.activeTextureUnit = Number(unit) - this.TEXTURE0;
        return oldActiveTexture.apply(this, arguments);
      };
      proto.bindTexture = function(target, texture) {
        const unit = tracked.activeTextureUnit || 0;
        if (!tracked.textureBindings[unit]) tracked.textureBindings[unit] = {};
        tracked.textureBindings[unit][target] = getTextureId(texture);
        return oldBindTexture.apply(this, arguments);
      };
      proto.vertexAttribPointer = function(index, size, type, normalized, stride, offset) {
        tracked.attribPointers[index] = { kind: 'float', size, type, normalized: !!normalized, stride, offset, buffer: tracked.arrayBuffer };
        return oldVertexAttribPointer.apply(this, arguments);
      };
      proto.vertexAttribIPointer = function(index, size, type, stride, offset) {
        tracked.attribPointers[index] = { kind: 'int', size, type, normalized: false, stride, offset, buffer: tracked.arrayBuffer };
        return oldVertexAttribIPointer.apply(this, arguments);
      };
      proto.enableVertexAttribArray = function(index) {
        tracked.attribPointers[index] = Object.assign({}, tracked.attribPointers[index] || {}, { enabled: true });
        return oldEnableVertexAttribArray.apply(this, arguments);
      };
      proto.disableVertexAttribArray = function(index) {
        tracked.attribPointers[index] = Object.assign({}, tracked.attribPointers[index] || {}, { enabled: false });
        return oldDisableVertexAttribArray.apply(this, arguments);
      };
      proto.bindFramebuffer = function(target, framebuffer) {
        if (target === this.FRAMEBUFFER || target === this.DRAW_FRAMEBUFFER) {
          boundFramebuffer = getFramebufferId(framebuffer);
        }
        return oldBindFramebuffer.apply(this, arguments);
      };
      proto.useProgram = function(program) {
        currentProgramId = getProgramId(program);
        currentProgramObj = program || null;
        state.useProgram++;
        if (state.programs.length < 80 && !state.programs.includes(currentProgramId)) state.programs.push(currentProgramId);
        return oldUseProgram.apply(this, arguments);
      };
      proto.drawElements = function() {
        state.drawElements++;
        sampleDraw('drawElements', arguments);
        const preErrors = drainErrors(this);
        const result = oldDrawElements.apply(this, arguments);
        const drawErrors = drainErrors(this);
        if (drawErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawElements.draw', error: drawErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        if (preErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawElements.pre', error: preErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        if (Number(arguments[1] || 0) >= 1000) {
          snapshotDrawState(this, 'drawElements-' + Number(arguments[1] || 0) + '-p' + currentProgramId, arguments, drawErrors, preErrors);
        }
        if (Number(arguments[1] || 0) >= 1000) sampleFramebuffer(this, 'after-drawElements-' + Number(arguments[1] || 0) + '-p' + currentProgramId);
        return result;
      };
      proto.drawElementsInstanced = function() {
        state.drawElementsInstanced++;
        sampleDraw('drawElementsInstanced', arguments);
        const preErrors = drainErrors(this);
        const result = oldDrawElementsInstanced.apply(this, arguments);
        const drawErrors = drainErrors(this);
        if (drawErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawElementsInstanced.draw', error: drawErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        if (preErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawElementsInstanced.pre', error: preErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        return result;
      };
      proto.drawArrays = function() {
        state.drawArrays++;
        sampleDraw('drawArrays', arguments);
        const preErrors = drainErrors(this);
        const result = oldDrawArrays.apply(this, arguments);
        const drawErrors = drainErrors(this);
        if (drawErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawArrays.draw', error: drawErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        if (preErrors.length && state.errors.length < 40) {
          state.errors.push({ kind: 'drawArrays.pre', error: preErrors, program: currentProgramId, t: Date.now() - state.installedAt });
        }
        return result;
      };
      proto.clear = function() {
        state.clear++;
        if (state.clearSamples.length < 80) state.clearSamples.push({ args: Array.from(arguments), t: Date.now() - state.installedAt });
        const result = oldClear.apply(this, arguments);
        sampleFramebuffer(this, 'after-clear-' + Array.from(arguments).join(','));
        return result;
      };
      proto.blitFramebuffer = function() {
        state.blitFramebuffer++;
        sampleDraw('blitFramebuffer', arguments);
        const result = oldBlit.apply(this, arguments);
        sampleFramebuffer(this, 'after-blit');
        return result;
      };
      proto.__webmcDrawProbeInstalled = true;
    };
    install();
  }, { stateDrawSampleLimit: STATE_DRAW_SAMPLE_LIMIT, deepDrawSampleLimit: DEEP_DRAW_SAMPLE_LIMIT });
  page = await context.newPage();
  page.on('console', (msg) => {
    const text = msg.text();
    consoleEvents.push(text);
    if (text.includes('[mc-web/worldload]') || text.includes('[mc-web/render/state]') || text.includes('[mc-web/gl]') || text.includes('[mc-web/diag]')) {
      console.log(text);
    }
  });

  const startedAt = new Date().toISOString();
  let readyAtMs = null;
  let runError = null;
  try {
    const startMs = Date.now();
    try {
      await page.goto(url, { waitUntil: 'commit', timeout: 90000 });
    } catch (err) {
      runError = `goto continued after: ${String(err && err.message || err)}`;
    }
    while (Date.now() - startMs < WAIT_MS) {
      const state = await readLatestState(page, beacons);
      if (sceneReady(state, consoleEvents)) {
        readyAtMs = Date.now() - startMs;
        await page.evaluate(() => {
          const state = globalThis.__webmcDrawProbe;
          if (!state) return;
          state.installedAt = Date.now();
          state.resetAtSceneReady = true;
          state.drawElements = 0;
          state.drawElementsInstanced = 0;
          state.drawArrays = 0;
          state.clear = 0;
          state.blitFramebuffer = 0;
          state.useProgram = 0;
          state.drawSamples = [];
          state.clearSamples = [];
          state.drawStateSamples = [];
          state.framebufferSamples = [];
          state.programs = [];
          state.errors = [];
        }).catch(() => {});
        break;
      }
      await page.waitForTimeout(1000);
    }
    await page.waitForTimeout(AFTER_READY_SAMPLE_MS);
  } catch (err) {
    runError = String(err && err.stack || err);
  }

  const screenshotPath = path.join(OUT_DIR, 'webgl-draw-probe-latest.png');
  let screenshotError = null;
  try {
    await writeCanvasPngFromPage(page, screenshotPath);
  } catch (err) {
    screenshotError = String(err && err.stack || err);
    const fallbackPng = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
      'base64'
    );
    fs.writeFileSync(screenshotPath, fallbackPng);
  }
  const drawProbe = await page.evaluate(() => globalThis.__webmcDrawProbe || null).catch((err) => ({ evaluateError: String(err && err.stack || err) }));
  report = {
    schemaVersion: 1,
    startedAt,
    finishedAt: new Date().toISOString(),
    url,
    readyAtMs,
    latestState: await readLatestState(page, beacons),
    screenshot: {
      path: screenshotPath,
      stats: analyzePng(screenshotPath),
      error: screenshotError
    },
    drawProbe,
    console: {
      gl: consoleEvents.filter((line) => line.includes('[mc-web/gl]')),
      worldload: consoleEvents.filter((line) => line.includes('[mc-web/worldload] probe success:'))
    },
    runError
  };
  const reportPath = path.join(OUT_DIR, 'webgl-draw-probe-latest.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`probe.readyAtMs ${readyAtMs}`);
  console.log(`probe.drawElements ${drawProbe && drawProbe.drawElements}`);
  console.log(`probe.drawArrays ${drawProbe && drawProbe.drawArrays}`);
  console.log(`probe.blitFramebuffer ${drawProbe && drawProbe.blitFramebuffer}`);
  console.log(`probe.errors ${drawProbe && drawProbe.errors ? drawProbe.errors.length : 'null'}`);
  console.log(`probe.buckets ${report.screenshot.stats.quantizedColorBuckets}`);
  console.log(`probe.edgeRatio ${report.screenshot.stats.edgeRatio.toFixed(4)}`);
  console.log(`probe.report ${reportPath}`);
  } finally {
    if (browser) await browser.close().catch(() => {});
    await new Promise((resolve) => server.close(resolve)).catch(() => {});
  }
}

main().catch((err) => {
  console.error(err && err.stack || String(err));
  process.exitCode = 1;
});
