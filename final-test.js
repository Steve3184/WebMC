const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const errors = [];
  const warnings = [];
  const logs = [];

  page.on('console', msg => {
    const msgText = msg.text();
    // Filter out noise
    if (msgText.includes('DeprecationWarning') || msgText.includes('node_modules')) return;

    if (msg.type() === 'error') {
      errors.push(msgText);
      console.error('[ERROR]', msgText);
    } else if (msg.type() === 'warning') {
      warnings.push(msgText);
      console.warn('[WARN]', msgText);
    } else {
      logs.push(msgText);
      if (msgText.includes('WebMC') || msgText.includes('Rendering') || msgText.includes('initialized')) {
        console.log('[LOG]', msgText);
      }
    }
  });

  page.on('pageerror', err => {
    errors.push(`PAGE ERROR: ${err.message}`);
    console.error('[PAGE ERROR]', err.message);
    console.error(err.stack);
  });

  try {
    console.log('=== 测试访问 WebMC ===\n');
    await page.goto('http://localhost:3001/', { waitUntil: 'networkidle', timeout: 20000 });
    await page.waitForTimeout(5000);

    // Mouse down to unlock (simulate user action)
    await page.mouse.down();
    await page.waitForTimeout(2000);

    const title = await page.title();
    console.log(`\n🎮 Title: ${title}`);

    // Click "Singleplayer"
    const singleplayerBtn = await page.getByText('Singleplayer', { exact: true }).first();
    if (singleplayerBtn) {
      console.log('✅ Found Singleplayer button');
      await singleplayerBtn.click({ delay: 200 });
      console.log('Clicked Singleplayer button');
      await page.waitForTimeout(3000);
    }

    // Wait for canvas to appear
    console.log('\n⏳ Waiting for canvas to appear...');
    await page.waitForFunction(() => {
      const cvs = document.querySelector('#canvas');
      return cvs && cvs.width > 0 && cvs.height > 0;
    }, { timeout: 20000 });

    console.log('✅ Canvas created!');

    // Get updated page info
    const finalState = await page.evaluate(() => {
      const canvas = document.querySelector('#canvas');
      return {
        canvas: canvas ? { width: canvas.width, height: canvas.height } : null,
        boot: !!document.querySelector('#boot')?.getAttribute('class')?.includes('hidden'),
        bodyText: document.body.innerText.substring(0, 1000)
      };
    });

    console.log('\n=== 最终状态 ===');
    console.log('Canvas:', finalState.canvas);
    console.log('Boot hidden:', finalState.boot);
    console.log('错误数量:', errors.length);
    console.log('警告数量:', warnings.length);

    if (errors.length === 0) {
      console.log('\n🎉 成功！无任何 JavaScript 错误！');
    } else {
      console.log('\n❌ 发现错误:');
      errors.slice(0, 5).forEach(err => console.log('  -', err.substring(0, 400)));
    }

  } catch (err) {
    console.error('\n⚠️ 测试过程中出错:', err.message);
  }

  await browser.close();
})();