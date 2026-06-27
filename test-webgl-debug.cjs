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
      '--disable-gpu-sandbox',
      '--disable-web-security'
    ]
  });
  const page = await browser.newPage();
  
  page.on('console', msg => {
    console.log('BROWSER [' + msg.type() + ']:', msg.text());
  });
  
  page.on('pageerror', err => {
    console.log('PAGE ERROR:', err.message);
  });
  
  console.log('Opening page...');
  await page.goto('http://localhost:58080/?boot=mcMain', { timeout: 30000 });
  
  // Wait for any output
  await page.waitForTimeout(3000);
  
  // Check if canvas exists
  const canvasExists = await page.evaluate(() => {
    const canvas = document.getElementById('game-canvas');
    return {
      exists: !!canvas,
      width: canvas?.width,
      height: canvas?.height,
      style: canvas?.style?.cssText
    };
  });
  console.log('Canvas state:', JSON.stringify(canvasExists));

  // Try to get WebGL2 context directly
  const webgl2Result = await page.evaluate(() => {
    const canvas = document.getElementById('game-canvas');
    if (!canvas) return { error: 'no canvas' };
    
    try {
      const gl = canvas.getContext('webgl2');
      return { 
        success: !!gl, 
        vendor: gl?.getParameter(gl?.VENDOR),
        renderer: gl?.getParameter(gl?.RENDERER),
        version: gl?.getParameter(gl?.VERSION)
      };
    } catch (e) {
      return { error: e.message };
    }
  });
  console.log('WebGL2 direct test:', JSON.stringify(webgl2Result));
  
  await browser.close();
}

test().catch(e => {
  console.error('Test error:', e.message);
  process.exit(1);
});
