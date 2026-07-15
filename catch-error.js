// 测试浏览器主动检查所有错误和加载状态
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false, slowMo: 50 });  // 使用非静默模式方便查看
  const page = await browser.newPage();

  page.on('console', msg => {
    const text = msg.text();
    if (msg.type() === 'error' && text.includes('Uncaught') && !text.includes('Decoding')) {
      console.error('[ERROR]', text);
    }
  });

  try {
    // 1. 先加载基本页面
    console.log('🔵 正在访问 http://localhost:3001/index.html');
    await page.goto('http://localhost:3001/index.html', { waitUntil: 'networkidle', timeout: 25000 });
    console.log('✓ 页面加载完成');

    // 2. 等待所有JavaScript执行
    console.log('⏳ 等待游戏初始化...');
    await page.waitForTimeout(8000);

    // 3. 获取 DOM 状态
    const status = await page.evaluate(() => {
      return {
        title: document.title,
        canvas: { present: !!document.getElementById('canvas') || false,
                 width: document.getElementById('canvas')?.width || 0,
                 height: document.getElementById('canvas')?.height || 0 },
        boot: document.getElementById('boot')?.innerText.substring(0, 200) || '',
        scriptsLoaded: Array.from(document.scripts).map(s => s.src.split('/').pop()).join(', '),
        loadingText: document.body.innerText.includes('Loading') || document.body.innerText.includes('Singleplayer')
      };
    });

    console.log('\n📋 游戏状态报告:');
    console.log(`   ❶ 标题: ${status.title}`);
    console.log(`   ❷ Canvas 画布: ${status.canvas.present ? `${status.canvas.width}x${status.canvas.height}` : '不存在'}`);
    console.log(`   ❸ 加载状态: ${status.loadingText ? '显示 Game/FPS/Loading' : '未知状态'}`);
    console.log(`   ❹ Pre-Singleplayer UI: ${status.boot}`);
    console.log(`   ❺ 加载的JS脚本: ${status.scriptsLoaded}`);

    const logContainer = document.getElementById('boot-status');
    const allLogs = logContainer?.innerText || '无信息';
    console.log(`   ❻ 加载状态文本: ${allLogs}`);

    // 4. 检查控制台 Console 获取的详细 JavaScript 执行信息
    console.log('\n🎯 基础检查完成。');
    console.log('请在浏览器中按 F12 检查:');
    console.log('- Console 标签页的 alert/错误信息');
    console.log('- Network 标签页是否加载 game.js 成功(状态码 200)');
    console.log('- Application 标签页是否有 localStorage 或 JS 报错(stack trace)');

    // 截图到／tmp/
    await page.screenshot({ path: 'M:/Users/l/Desktop/webmc1/test-screenshot.png', fullPage: true });
    console.log('\n✉️ 已保存截图到: M:/Users/l/Desktop/webmc1/test-screenshot.png');

  } catch (err) {
    console.error('❌ 测试失败:', err.message);
  } finally {
    await browser.close();
  }
})();