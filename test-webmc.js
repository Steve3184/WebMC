const { chromium } = require('playwright');

(async () => {
    console.log('启动 Playwright 浏览器测试...\n');

    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    console.log('访问 http://localhost:8888...\n');

    // 监听控制台消息
    page.on('pageerror', err => {
        console.error('页面错误:', err.message);
        console.error('Stack:', err.stack);
    });

    page.on('console', msg => {
        const text = msg.text();
        if (text.includes('Error') && !text.includes('deprecated') && !text.includes('DeprecationWarning')) {
            console.error('控制台错误:', text);
        } else if (text.includes('[WebMC]') || text.includes('Loaded')) {
            console.log(text);
        }
    });

    // 加载页面
    const response = await page.goto('http://localhost:8888', { waitUntil: 'networkidle', timeout: 30000 });

    console.log(`\n✅ 页面加载完成: ${response.status()} - ${response.statusText()}`);
    console.log(`Title: ${await page.title()}`);

    // 等待 10 秒让 JS 执行
    await page.waitForTimeout(10000);

    // 获取错误状态
    try {
        await page.waitForFunction(() => document.getElementById('boot-error').className.includes('show'), { timeout: 1000 });
        const bootError = await page.$eval('#boot-error', el => el.textContent);
        if (bootError.trim()) {
            console.error('\n❌ 启动错误:\n' + bootError);
        }
    } catch (e) {
        console.log('\n✅ 启动过程正常（无错误弹窗）');
    }

    console.log('\n✅ 测试完成！');

    // 截图用于确认
    await page.screenshot({ path: 'webmc-test-screenshot.png', fullPage: true });
    console.log('截图已保存到: webmc-test-screenshot.png');

    await browser.close();
})();
