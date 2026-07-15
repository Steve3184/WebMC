const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  console.log('访问 http://localhost:3001/');
  await page.goto('http://localhost:3001/');
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'M:/Users/l/Desktop/webmc1/screenshot.png' });
  console.log('页面文字:', await page.evaluate(() => document.body.innerText));
  await browser.close();
})();
