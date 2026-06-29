const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: false,
    args: ['--no-sandbox']
  });
  const page = await browser.newPage();

  console.log('Loading WebMC...');

  const logs = [];
  const errors = [];

  page.on('console', msg => {
    const text = msg.text();
    logs.push({ type: msg.type(), text });
  });

  page.on('pageerror', err => {
    errors.push(err.message);
  });

  try {
    await page.goto('http://localhost:9090/', { timeout: 60000 });
    console.log('Page loaded');

    await page.waitForTimeout(30000);

    const title = await page.title();
    console.log('Title:', title);

    const canvas = await page.$('#game-canvas');
    console.log('Canvas:', canvas ? 'exists' : 'missing');

    // Check for BigInt/NaN errors
    const bigintErrors = errors.filter(e =>
      e.includes('BigInt') || e.includes('NaN') || e.includes('Bigint')
    );

    console.log('\n=== BigInt/NaN Errors ===');
    console.log(`Count: ${bigintErrors.length}`);
    bigintErrors.slice(0, 3).forEach(e => console.log('  ' + e.substring(0, 150)));

    console.log('\n=== All Errors ===');
    console.log(`Count: ${errors.length}`);
    errors.slice(0, 5).forEach(e => console.log('  ' + e.substring(0, 150)));

    // Check boot state
    const bootVisible = await page.locator('#boot').isVisible().catch(() => false);
    const bootStatus = await page.locator('#boot-status').textContent().catch(() => 'N/A');
    const bootError = await page.locator('#boot-error').isVisible().catch(() => false);
    const bootErrorText = await page.locator('#boot-error').textContent().catch(() => '');

    console.log('\n=== Boot State ===');
    console.log(`Boot visible: ${bootVisible}`);
    console.log(`Status: ${bootStatus}`);
    console.log(`Error visible: ${bootError}`);
    if (bootError) console.log(`Error: ${bootErrorText.substring(0, 200)}`);

  } catch (err) {
    console.error('Test failed:', err.message);
  } finally {
    await browser.close();
  }

  const bigintErrors = errors.filter(e =>
    e.includes('BigInt') || e.includes('NaN') || e.includes('Bigint')
  );

  if (bigintErrors.length > 0) {
    console.log('\n❌ FAIL: BigInt/NaN errors found');
    process.exit(1);
  } else {
    console.log('\n✅ PASS: No BigInt/NaN errors');
    process.exit(0);
  }
})();
