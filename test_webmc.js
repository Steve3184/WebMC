const { chromium } = require('playwright');
const path = require('path');
const http = require('http');
const fs = require('fs');

const PORT = 8765;
const WEB_DIR = path.join(__dirname, 'addons/web');

// 简单的静态服务器
const server = http.createServer((req, res) => {
    let filePath = path.join(WEB_DIR, req.url === '/' ? 'index.html' : req.url);
    const ext = path.extname(filePath);
    const mimeTypes = {
        '.html': 'text/html',
        '.js': 'application/javascript',
        '.css': 'text/css',
        '.json': 'application/json',
    };

    fs.readFile(filePath, (err, content) => {
        if (err) {
            res.writeHead(404);
            res.end('Not found');
            return;
        }
        res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'text/plain' });
        res.end(content);
    });
});

async function testWebMC() {
    console.log('Starting WebMC test...\n');

    await new Promise(resolve => server.listen(PORT, resolve));
    console.log(`Server running at http://localhost:${PORT}`);

    const browser = await chromium.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext();
    const page = await context.newPage();

    const errors = [];
    const warnings = [];
    const logs = [];

    // 监听控制台消息
    page.on('console', msg => {
        const type = msg.type();
        const text = msg.text();
        if (type === 'error') {
            errors.push(text);
        } else if (type === 'warning') {
            warnings.push(text);
        } else {
            logs.push(`[${type}] ${text}`);
        }
    });

    // 监听页面错误
    page.on('pageerror', err => {
        errors.push(`Page Error: ${err.message}`);
    });

    try {
        console.log('Loading page...');
        await page.goto(`http://localhost:${PORT}`, { waitUntil: 'networkidle', timeout: 30000 });
        console.log('Page loaded.\n');

        // 等待一下让游戏初始化
        await page.waitForTimeout(5000);

        // 输出结果
        console.log('=== Console Logs ===');
        logs.slice(0, 20).forEach(l => console.log(l));
        if (logs.length > 20) console.log(`... and ${logs.length - 20} more logs`);

        console.log('\n=== Warnings ===');
        warnings.slice(0, 10).forEach(w => console.log(w));
        if (warnings.length > 10) console.log(`... and ${warnings.length - 10} more warnings`);

        console.log('\n=== ERRORS ===');
        if (errors.length === 0) {
            console.log('No errors found!');
        } else {
            errors.forEach(e => console.log(e));
        }

        // 检查 canvas 状态
        const canvasStatus = await page.evaluate(() => {
            const canvas = document.getElementById('game-canvas');
            if (!canvas) return 'Canvas not found';
            const ctx = canvas.getContext('webgl') || canvas.getContext('webgl2') || canvas.getContext('2d');
            return {
                width: canvas.width,
                height: canvas.height,
                hasContext: !!ctx,
                contextType: ctx ? (ctx instanceof WebGLRenderingContext ? 'webgl' : ctx instanceof WebGL2RenderingContext ? 'webgl2' : '2d') : 'none'
            };
        });
        console.log('\n=== Canvas Status ===');
        console.log(JSON.stringify(canvasStatus, null, 2));

    } catch (err) {
        console.error('Test failed:', err.message);
    } finally {
        await browser.close();
        server.close();
        console.log('\nTest completed.');
    }
}

testWebMC().catch(console.error);
