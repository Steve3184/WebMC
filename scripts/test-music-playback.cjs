const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROOT = path.join(process.cwd(), 'work', 'build', 'web-run');
const PORT = 8082;
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
const beacons = [];

function serveStatic(req, res) {
  const parsed = new URL(req.url || '/', 'http://localhost');
  if (parsed.pathname === '/__webmc_state') {
    let state = null;
    const raw = parsed.searchParams.get('d');
    if (raw) {
      try { state = JSON.parse(raw); } catch { state = { parseError: true, raw }; }
    }
    beacons.push({ receivedAt: Date.now(), source: parsed.searchParams.get('source') || '', state });
    res.writeHead(204);
    res.end();
    return;
  }
  let p = req.url.split('?')[0];
  if (p === '/') p = '/index.html';
  const file = path.join(ROOT, p);
  if (!file.startsWith(ROOT)) { res.writeHead(403); res.end(); return; }
  if (!fs.existsSync(file)) { res.writeHead(404); res.end(); return; }
  const ext = path.extname(file).toLowerCase();
  const ct = MIME[ext] || 'application/octet-stream';
  const s = fs.createReadStream(file);
  res.writeHead(200, { 'Content-Type': ct, 'Cache-Control': 'no-cache' });
  s.pipe(res);
}

const server = http.createServer(serveStatic);
server.listen(PORT, async () => {
  console.log(`Server on :${PORT}`);
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
  const logs = [];
  page.on('console', msg => {
    const t = msg.type();
    const txt = msg.text();
    logs.push({ t, txt });
    if (t === 'error' || t === 'exception') console.error('[BROWSER-ERR]', txt);
    else if (txt.includes('music') || txt.includes('SoundBuffer') || txt.includes('webPlay') || txt.includes('play result') || txt.includes('UnsupportedOperation') || txt.includes('buffer ready') || txt.includes('ticker') || txt.includes('AudioContext')) console.log('[BROWSER]', txt);
  });
  page.on('pageerror', err => console.error('[PAGE-ERR]', err.message));

  console.log('Navigating...');
  await page.goto(`http://localhost:${PORT}/?boot=mcMain&t=${Date.now()}`, { waitUntil: 'domcontentloaded', timeout: 90000 });

  async function waitForState(predicate, timeoutMs, label) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      const state = await page.evaluate(() => window.__webmcState || {}).catch(() => ({}));
      if (predicate(state)) {
        const elapsed = timeoutMs - (deadline - Date.now());
        console.log(`State condition met [${label}] after ${elapsed}ms`);
        return state;
      }
      await page.waitForTimeout(1000);
    }
    throw new Error(`Timeout [${label}] after ${timeoutMs}ms`);
  }

  console.log('Waiting for TitleScreen...');
  const titleState = await waitForState(s => s.screen === 'TitleScreen', 120000, 'TitleScreen');
  console.log('TitleScreen ready, clicking Singleplayer...');
  await page.locator('#webmc-main-menu button').click({ timeout: 30000 });
  console.log('Clicked Singleplayer, waiting for world load...');
  const worldState = await waitForState(s => s.levelPresent && s.playerPresent && s.screen === 'null', 600000, 'WorldReady');
  console.log('World loaded! State:', JSON.stringify({ screen: worldState.screen, levelPresent: worldState.levelPresent, playerPresent: worldState.playerPresent, renderWorld: worldState.renderWorld }));
  const worldMs = Date.now();
  const waitMusicMs = 210000;
  console.log(`Waiting ${waitMusicMs}ms for music...`);
  let lastTicker = -1;
  while (Date.now() - worldMs < waitMusicMs) {
    await page.waitForTimeout(5000);
    const state = await page.evaluate(() => window.__webmcState || {}).catch(() => ({}));
    const webMusicPlaying = state.webMusicPlaying;
    const webCurrentSound = state.webCurrentSound;
    const ticker = state.webMcMusicTicker;
    console.log(`ticker=${ticker} webMusicPlaying=${webMusicPlaying} webCurrentSound=${webCurrentSound}`);
    if (webMusicPlaying) {
      console.log('*** MUSIC IS PLAYING ***');
      break;
    }
    if (ticker > lastTicker) {
      lastTicker = ticker;
      console.log(`music ticker: ${ticker}`);
    }
    const playLogs = logs.filter(l => l.txt.includes('play result') || l.txt.includes('buffer ready') || l.txt.includes('Unsupported'));
    if (playLogs.length > 0) {
      playLogs.forEach(l => console.log(`[${l.t}] ${l.txt}`));
    }
  }

  await page.screenshot({ path: 'output/test-music-final.png' }).catch(() => {});
  console.log('\n=== All music-related logs ===');
  logs.filter(l => l.txt.toLowerCase().includes('music') || l.txt.includes('play result') || l.txt.includes('buffer ready') || l.txt.includes('ticker') || l.txt.includes('SoundBuffer') || l.txt.includes('Unsupported') || l.txt.includes('AudioContext')).forEach(l => console.log(`[${l.t}] ${l.txt}`));
  const errors = logs.filter(l => l.t === 'error' || l.t === 'exception');
  if (errors.length > 0) { console.log('\n=== Errors ==='); errors.forEach(l => console.log(l.txt)); }
  const state = await page.evaluate(() => window.__webmcState || {});
  console.log('\n=== Final webmcState keys ===');
  Object.keys(state).sort().forEach(k => {
    if (typeof state[k] !== 'object' || state[k] === null) console.log(`  ${k}: ${JSON.stringify(state[k])}`);
  });
  await browser.close();
  server.close();
  process.exit(0);
});
