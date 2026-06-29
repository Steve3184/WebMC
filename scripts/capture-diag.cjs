const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { chromium } = require('playwright');

const REPO_ROOT = path.resolve(__dirname, '..');
const WORK_DIR = path.join(REPO_ROOT, 'work');
const ROOT = path.join(WORK_DIR, 'build', 'web-run');
const PORT = Number(process.env.WEBMC_PORT || 58080);
const BASE_URL = 'http://localhost:' + PORT + '/';
const TIMEOUT_MS = Number(process.env.TIMEOUT_MS || 120000);

const allLines = [];

function delay(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function waitForServer(url, timeoutMs) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    try {
      const res = await fetch(url);
      const body = await res.text();
      if (res.ok && body.includes('bootstrap.js')) return true;
    } catch (_) {}
    await delay(250);
  }
  return false;
}

async function main() {
  console.log('[diag-capture] starting server...');
  const server = spawn(process.execPath, [path.join(WORK_DIR, 'serve-web-run.cjs')], {
    cwd: WORK_DIR,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env, WEBMC_PORT: String(PORT) }
  });
  server.stdout.on('data', d => {});
  server.stderr.on('data', d => {});

  const healthy = await waitForServer(BASE_URL, 15000);
  if (!healthy) {
    console.error('[diag-capture] server not healthy');
    server.kill();
    process.exit(1);
  }
  console.log('[diag-capture] server ok');

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  page.on('console', msg => {
    const line = msg.text();
    allLines.push(line);
    if (line.includes('[diag]') || line.includes('[mc-probe] reload.state') || line.includes('[mc-probe] prepareTasks')) {
      console.log('[BROWSER] ' + line);
    }
  });
  page.on('pageerror', err => {
    console.log('[PAGE_ERROR] ' + (err && err.stack ? err.stack : String(err)));
  });

  await page.addInitScript(() => {
    window.webmcBootMode = 'mcMain';
    window.webmcDiagnostics = true;
  });

  const url = BASE_URL + '?boot=mcMain&diagnostics=1&t=' + Date.now();
  console.log('[diag-capture] navigating to ' + url);
  await page.goto(url, { waitUntil: 'commit', timeout: 20000 });

  const deadline = Date.now() + TIMEOUT_MS;
  let lastLineCount = 0;
  let stallCount = 0;

  while (Date.now() < deadline) {
    await page.waitForTimeout(1000);

    const newLines = allLines.slice(lastLineCount);
    lastLineCount = allLines.length;

    const hasDiagActivity = newLines.some(l => l.includes('[diag]'));
    if (hasDiagActivity) {
      stallCount = 0;
    } else {
      stallCount++;
    }

    if (allLines.some(l => l.includes('allPreparations COMPLETION'))) {
      console.log('[diag-capture] allPreparations completed, waiting 10s for remaining diags...');
      await page.waitForTimeout(10000);
      break;
    }

    if (stallCount >= 30) {
      const hasReloadDiags = allLines.some(l => l.includes('[diag] barrier.wait') || l.includes('[diag] APPLY'));
      if (hasReloadDiags) {
        console.log('[diag-capture] no new diag for 30s, likely done, dumping');
        break;
      }
    }
  }

  console.log('\n========== COMPLETE [diag] LOG ==========');
  for (const line of allLines) {
    if (line.includes('[diag]')) {
      console.log(line);
    }
  }
  console.log('========== END [diag] LOG ==========');

  console.log('\n========== ALL reload.state LINES ==========');
  for (const line of allLines) {
    if (line.includes('[mc-probe] reload.state')) {
      console.log(line);
    }
  }
  console.log('========== END reload.state ==========');

  await browser.close();
  server.kill();
  process.exit(0);
}

main().catch(err => {
  console.error('[diag-capture] error:', err);
  process.exit(1);
});
