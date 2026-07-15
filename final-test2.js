const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const errors = [];
  let hasCriticalError = false;

  page.on('console', msg => {
    const msgText = msg.text();
    if (msg.type() === 'error' && !msgText.includes('punycode') && !msgText.includes('deprecated')) {
      errors.push(msgText);
      hasCriticalError = true;
    }
  });

  page.on('pageerror', err => {
    errors.push(`PAGE ERROR: ${err.message}`);
    hasCriticalError = true;
  });

  try {
    console.log('🎮 访问 http://localhost:3001/');
    await page.goto('http://localhost:3001/', { waitUntil: 'networkidle', timeout: 20000 });
    await page.waitForTimeout(8000);

    // Check game state
    const state = await page.evaluate(() => {
      return {
        title: document.title,
        hasCanvas: !!document.querySelector('#canvas'),
        canvas: document.querySelector('#canvas'),
        bootText: document.querySelector('#boot')?.innerText || '',
        scripts: document.scripts.length,
        bodyText: document.body.innerText.substring(0, 500)
      };
    });

    console.log('\n📊 游戏状态:');
    console.log(`   标题: ${state.title}`);
    console.log(`   Canvas 存在: ${state.hasCanvas}`);
    console.log(`   脚本数量: ${state.scripts}`);
    console.log(`   页面内容: ${state.bodyText}`);

    // Click Singleplayer
    console.log('\n🖱️ 点击 Singleplayer...');
    await page.click('text=Singleplayer');
    await page.waitForTimeout(5000);

    const afterClick = await page.evaluate(() => ({
      bodyText: document.body.innerText.substring(0, 800)
    }));
    console.log(`\n📝 点击后内容:\n${afterClick.bodyText}`);

    console.log('\n' + '='.repeat(50));
    if (errors.length === 0 && !hasCriticalError) {
      console.log('✅ ✅ ✅ 测试通过！零错误！');
    } else {
      console.log(`❌ 发现 ${errors.length} 个错误:`);
      errors.forEach(e => console.log('   -', e.substring(0, 200)));
    }
    console.log('='.repeat(50));

  } catch (err) {
    console.error('测试出错:', err.message);
  }

  await browser.close();
})();