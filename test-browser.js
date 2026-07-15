const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const errors = [];
  const logs = [];

  // Capture console messages
  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    if (type === 'error') {
      errors.push(text);
    }
    logs.push(`[${type}] ${text}`);
  });

  // Capture page errors
  page.on('pageerror', err => {
    errors.push(`PAGE ERROR: ${err.message}`);
  });

  try {
    console.log('Navigating to http://localhost:3001/index.html...');
    await page.goto('http://localhost:3001/index.html', {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    });

    // Wait for game to initialize
    console.log('Waiting for game initialization...');
    await page.waitForTimeout(10000);

    // Get page title and body text
    const title = await page.title();
    const bodyText = await page.evaluate(() => document.body.innerText.substring(0, 500));
    const gameState = await page.evaluate(() => {
      return {
        hasWebGL: typeof WebGLRenderingContext !== 'undefined',
        windowMain: typeof window.main !== 'undefined',
        gameJsLoaded: document.querySelector('script[src="game.js"]') !== null,
        canvasPresent: document.querySelector('#canvas') !== null,
        bootVisible: document.querySelector('#boot:not(.hidden)') !== null
      };
    });

    console.log('\n=== PAGE STATE ===');
    console.log('Title:', title);
    console.log('Game State:', JSON.stringify(gameState, null, 2));
    console.log('\nBody Text (first 500 chars):', bodyText.substring(0, 300));

    console.log('\n=== ERRORS (count:', errors.length, ') ===');
    if (errors.length > 0) {
      errors.forEach((err, i) => console.log(`${i + 1}. ${err}`));
    } else {
      console.log('No errors!');
    }

    console.log('\n=== CONSOLE LOGS (last 30) ===');
    logs.slice(-30).forEach(log => console.log(log));

  } catch (err) {
    console.error('Navigation error:', err.message);
  }

  await browser.close();
})();
