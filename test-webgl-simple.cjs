const { chromium } = require('playwright');

async function test() {
  const browser = await chromium.launch({
    headless: true,
    args: [
      '--enable-webgl',
      '--enable-webgl2',
      '--use-gl=angle',
      '--use-angle=swiftshader',
      '--ignore-gpu-blocklist',
      '--disable-gpu-sandbox'
    ]
  });
  const page = await browser.newPage();
  
  let found = false;
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('WebGL2') || text.includes('no canvas')) {
      console.log('BROWSER:', text);
      found = true;
    }
  });
  
  console.log('Opening page...');
  await page.goto('http://localhost:58080/?boot=mcMain', { timeout: 30000 });
  
  // Wait for console output
  await page.waitForTimeout(5000);
  
  await browser.close();
  console.log(found ? 'Test passed - WebGL2 found' : 'Test warning - no WebGL2 message');
}

test().catch(e => {
  console.error('Test error:', e.message);
  process.exit(1);
});
