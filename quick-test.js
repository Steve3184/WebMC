const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('[JS ERROR]', msg.text());
    }
  });

  page.on('pageerror', err => {
    console.log('[PAGE ERROR]', err.message);
  });

  console.log('打开 http://localhost:3001/index.html...');
  await page.goto('http://localhost:3001/index.html', { timeout: 15000 });
  console.log('页面加载完成，等待 10 秒...');
  await page.waitForTimeout(10000);

  // 截图
  await page.screenshot({ path: 'M:/Users/l/Desktop/webmc1/screenshot.png', fullPage: true });
  console.log('截图已保存到 M:/Users/l/Desktop/webmc1/screenshot.png');

  // 获取页面文字
  const text = await page.evaluate(() => document.body.innerText);
  console.log('\n页面显示内容:');
  console.log(text);

  await browser.close();
})();
