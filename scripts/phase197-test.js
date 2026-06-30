/**
 * phase-197 验证测试
 * 验证 Minecraft 1.21.8 WebMC 浏览器端加载流程
 *
 * Phase 197 = Minecraft Main.main 执行入口点
 * 测试目标: 确认游戏成功加载到主菜单
 */

const { chromium } = require('playwright');

const SERVER_URL = 'http://localhost:8080';
const TIMEOUT = 120000; // 2分钟超时

async function runPhase197Test() {
    console.log('[phase-197] 启动浏览器测试...');

    const browser = await chromium.launch({
        headless: false, // WebGL2 需要有头模式
        args: ['--enable-webgl', '--use-gl=swiftshader']
    });

    const context = await browser.newContext({
        viewport: { width: 1280, height: 720 }
    });

    const page = await context.newPage();

    // 收集控制台日志
    const logs = [];
    const errors = [];

    page.on('console', msg => {
        const text = msg.text();
        logs.push({ type: msg.type(), text });
        if (msg.type() === 'error') {
            errors.push(text);
        }
    });

    page.on('pageerror', err => {
        errors.push(err.message);
    });

    try {
        console.log('[phase-197] 访问 WebMC...');
        await page.goto(SERVER_URL, { timeout: TIMEOUT });

        // 等待游戏加载完成
        console.log('[phase-197] 等待游戏加载...');

        // 等待 Singleplayer 按钮出现
        const singleplayerButton = await page.waitForSelector('button:has-text("Singleplayer")', {
            timeout: TIMEOUT
        });

        console.log('[phase-197] ✓ 主菜单已显示 - Singleplayer 按钮可见');

        // 检查 WebGL 状态
        const webglStatus = await page.evaluate(() => {
            const canvas = document.getElementById('game-canvas') || document.getElementById('canvas');
            const gl = canvas && (canvas.getContext('webgl2') || canvas.getContext('webgl'));
            return {
                canvasPresent: !!canvas,
                glAvailable: !!gl,
                canvasWidth: canvas ? canvas.width : 0,
                canvasHeight: canvas ? canvas.height : 0
            };
        });

        console.log('[phase-197] WebGL 状态:', webglStatus);

        // 检查音频状态 - 音频需要用户交互才能解锁，这是正常的
        const audioStatus = await page.evaluate(() => {
            const ctx = window.__webmcAudioContext;
            return ctx ? ctx.state : 'unavailable';
        });

        console.log('[phase-197] 音频状态:', audioStatus, '(suspended 是正常的，需要用户交互)');

        // 验证关键元素
        const checks = {
            'Singleplayer按钮': !!singleplayerButton,
            'Canvas存在': webglStatus.canvasPresent,
            'Canvas有尺寸': webglStatus.canvasWidth > 0 && webglStatus.canvasHeight > 0
        };

        console.log('\n[phase-197] 验证结果:');
        let allPassed = true;
        for (const [name, passed] of Object.entries(checks)) {
            const status = passed ? '✓' : '✗';
            console.log(`  ${status} ${name}`);
            if (!passed) allPassed = false;
        }

        if (allPassed) {
            console.log('\n[phase-197] ✅ 所有检查通过!');
        } else {
            console.log('\n[phase-197] ⚠️ 部分检查未通过');
        }

        // 输出关键错误（如果有）
        if (errors.length > 0) {
            console.log('\n[phase-197] 关键错误:');
            errors.slice(0, 5).forEach(e => console.log('  -', e.substring(0, 200)));
        }

        await browser.close();

        return allPassed ? 0 : 1;

    } catch (err) {
        console.error('[phase-197] ✗ 测试失败:', err.message);

        // 输出最近的日志
        console.log('\n最近的控制台日志:');
        logs.slice(-10).forEach(l => {
            console.log(`  [${l.type}] ${l.text.substring(0, 150)}`);
        });

        await browser.close();
        return 1;
    }
}

runPhase197Test()
    .then(code => process.exit(code))
    .catch(err => {
        console.error('Fatal:', err);
        process.exit(1);
    });
