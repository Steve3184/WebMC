const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const errors = [];
  const logs = [];

  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    if (type === 'error') {
      errors.push(text);
    }
    logs.push(`[${type}] ${text}`);
  });

  page.on('pageerror', err => {
    errors.push(`PAGE ERROR: ${err.message}`);
  });

  try {
    console.log('Navigating to http://localhost:3001/...');
    const response = await page.goto('http://localhost:3001/', {
      waitUntil: 'networkidle',
      timeout: 30000
    });
    console.log('Response status:', response.status());

    // Wait for game to load
    console.log('Waiting for game initialization (15s)...');
    await page.waitForTimeout(15000);

    // Get body content
    const bodyText = await page.evaluate(() => document.body.innerText);

    console.log('\n=== PAGE CONTENT ===');
    console.log(bodyText);

    console.log('\n=== ERRORS (count:', errors.length, ') ===');
    if (errors.length > 0) {
      errors.slice(0, 10).forEach((err, i) => console.log(`${i + 1}. ${err}`));
    } else {
      console.log('No errors!');
    }

    // Check for key elements
    const state = await page.evaluate(() => {
      return {
        hasCanvas: !!document.querySelector('#canvas'),
        hasBoot: !!document.querySelector('#boot'),
        bodyChildren: document.body.children.length,
        scripts: Array.from(document.scripts).map(s => s.src.split('/').pop())
      };
    });
    console.log('\n=== DOM STATE ===');
    console.log(JSON.stringify(state, null, 2));

  } catch (err) {
    console.error('Error:', err.message);
  }

  await browser.close();
})();
