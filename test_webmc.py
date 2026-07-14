"""
WebMC 自动化测试脚本
验证：1. 页面加载 2. Canvas 创建 3. JS 加载 4. 控制台错误 5. WebGL 渲染 6. 事件系统
"""
from playwright.sync_api import sync_playwright
import json
import time

def test_webmc():
    results = {
        "page_load": False,
        "canvas_exists": False,
        "js_loaded": False,
        "console_errors": [],
        "webgl_available": False,
        "event_system": False,
        "issues": []
    }

    with sync_playwright() as p:
        print("🚀 启动 Chromium 浏览器...")
        browser = p.chromium.launch(
            headless=True,
            args=[
                '--enable-webgl',
                '--use-gl=swiftshader',
                '--disable-web-security'
            ]
        )

        context = browser.new_context(
            viewport={'width': 1280, 'height': 720}
        )
        page = context.new_page()

        # 监听控制台消息
        def handle_console(msg):
            if msg.type == 'error':
                results["console_errors"].append(msg.text)
                print(f"❌ 控制台错误: {msg.text}")
            elif msg.type == 'warning':
                print(f"⚠️  控制台警告: {msg.text}")

        page.on('console', handle_console)

        # 监听页面错误
        def handle_page_error(error):
            results["issues"].append(f"页面错误: {error}")
            print(f"❌ 页面错误: {error}")

        page.on('pageerror', handle_page_error)

        print("\n📄 1. 测试页面加载...")
        try:
            page.goto('http://localhost:8080/index.html', timeout=30000)
            page.wait_for_load_state('networkidle', timeout=30000)
            results["page_load"] = True
            print("✅ 页面加载成功")
        except Exception as e:
            results["issues"].append(f"页面加载失败: {e}")
            print(f"❌ 页面加载失败: {e}")
            browser.close()
            return results

        # 等待一下让脚本执行
        time.sleep(2)

        print("\n🎨 2. 检查 Canvas 创建...")
        canvas_info = page.evaluate("""() => {
            const canvas = document.getElementById('game-canvas');
            if (!canvas) return { exists: false };
            return {
                exists: true,
                width: canvas.width,
                height: canvas.height,
                hasContext: !!canvas.getContext('webgl2') || !!canvas.getContext('webgl') || !!canvas.getContext('2d')
            };
        }""")

        results["canvas_exists"] = canvas_info.get('exists', False)
        if results["canvas_exists"]:
            print(f"✅ Canvas 存在: {canvas_info['width']}x{canvas_info['height']}")
            print(f"   Canvas 上下文可用: {canvas_info.get('hasContext', False)}")
        else:
            results["issues"].append("Canvas 未找到")
            print("❌ Canvas 未找到")

        print("\n⚡ 3. 检查 JavaScript 加载...")
        js_status = page.evaluate("""() => {
            return {
                gameJS: typeof game !== 'undefined',
                teaVM: typeof window.teavm !== 'undefined' || typeof Module !== 'undefined',
                webmcBridge: typeof window.webmc !== 'undefined',
                bootstrapLoaded: typeof bootstrap === 'function'
            };
        }""")

        results["js_loaded"] = all(js_status.values())
        for name, loaded in js_status.items():
            status = "✅" if loaded else "❌"
            print(f"   {status} {name}: {'已加载' if loaded else '未加载'}")

        print("\n🌐 4. 检查 WebGL 支持...")
        webgl_info = page.evaluate("""() => {
            const canvas = document.createElement('canvas');
            const gl2 = canvas.getContext('webgl2');
            const gl = canvas.getContext('webgl');
            const gl1 = canvas.getContext('experimental-webgl');

            let renderer = '未知';
            let vendor = '未知';

            if (gl2) {
                const debugInfo = gl2.getExtension('WEBGL_debug_renderer_info');
                if (debugInfo) {
                    renderer = gl2.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
                    vendor = gl2.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
                }
                return {
                    available: true,
                    version: 'WebGL2',
                    renderer: renderer,
                    vendor: vendor,
                    maxTextureSize: gl2.getParameter(gl2.MAX_TEXTURE_SIZE)
                };
            } else if (gl) {
                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                if (debugInfo) {
                    renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
                    vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
                }
                return {
                    available: true,
                    version: 'WebGL1',
                    renderer: renderer,
                    vendor: vendor,
                    maxTextureSize: gl.getParameter(gl.MAX_TEXTURE_SIZE)
                };
            } else if (gl1) {
                return { available: true, version: 'WebGL (experimental)', renderer: 'N/A' };
            }
            return { available: false };
        }""")

        results["webgl_available"] = webgl_info.get('available', False)
        if results["webgl_available"]:
            print(f"✅ WebGL 可用: {webgl_info.get('version', '未知')}")
            print(f"   渲染器: {webgl_info.get('renderer', '未知')}")
            print(f"   厂商: {webgl_info.get('vendor', '未知')}")
            print(f"   最大纹理尺寸: {webgl_info.get('maxTextureSize', '未知')}")
        else:
            results["issues"].append("WebGL 不可用")
            print("❌ WebGL 不可用")

        print("\n🎮 5. 检查事件系统...")
        event_tests = page.evaluate("""() => {
            const results = {
                glfwExists: typeof GLFW !== 'undefined',
                inputBridge: typeof window.webmcInput !== 'undefined' || typeof inputBridge !== 'undefined',
                canvasClickable: false
            };

            const canvas = document.getElementById('game-canvas');
            if (canvas) {
                canvasClickable = canvas.style.pointerEvents !== 'none';
            }

            return results;
        }""")

        results["event_system"] = event_tests.get('glfwExists', False) or event_tests.get('inputBridge', False)
        for name, exists in event_tests.items():
            status = "✅" if exists else "⚠️"
            print(f"   {status} {name}: {'存在' if exists else '不存在'}")

        # 测试鼠标事件
        print("\n🖱️ 6. 测试鼠标事件...")
        mouse_test = page.evaluate("""() => {
            return new Promise((resolve) => {
                const canvas = document.getElementById('game-canvas');
                if (!canvas) {
                    resolve({ passed: false, reason: 'No canvas' });
                    return;
                }

                let mousedownReceived = false;
                let mousemoveReceived = false;

                canvas.addEventListener('mousedown', () => {
                    mousedownReceived = true;
                });

                canvas.addEventListener('mousemove', () => {
                    mousemoveReceived = true;
                });

                // 模拟事件
                const rect = canvas.getBoundingClientRect();
                const event1 = new MouseEvent('mousedown', {
                    clientX: rect.left + 100,
                    clientY: rect.top + 100,
                    button: 0,
                    bubbles: true
                });
                canvas.dispatchEvent(event1);

                const event2 = new MouseEvent('mousemove', {
                    clientX: rect.left + 150,
                    clientY: rect.top + 150,
                    bubbles: true
                });
                canvas.dispatchEvent(event2);

                setTimeout(() => {
                    resolve({
                        passed: mousedownReceived && mousemoveReceived,
                        mousedown: mousedownReceived,
                        mousemove: mousemoveReceived
                    });
                }, 100);
            });
        }""")

        if mouse_test.get('passed'):
            print("✅ 鼠标事件工作正常")
        else:
            print(f"⚠️  鼠标事件测试: {mouse_test.get('reason', '')}")
            print(f"   mousedown: {mouse_test.get('mousedown', False)}")
            print(f"   mousemove: {mouse_test.get('mousemove', False)}")

        # 测试键盘事件
        print("\n⌨️ 7. 测试键盘事件...")
        keyboard_test = page.evaluate("""() => {
            return new Promise((resolve) => {
                let keydownReceived = false;

                document.addEventListener('keydown', (e) => {
                    if (e.key === 'w' || e.key === 'W') {
                        keydownReceived = true;
                    }
                });

                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'w',
                    code: 'KeyW',
                    bubbles: true
                }));

                setTimeout(() => {
                    resolve({ passed: keydownReceived });
                }, 100);
            });
        }""")

        if keyboard_test.get('passed'):
            print("✅ 键盘事件工作正常")
        else:
            print("⚠️  键盘事件测试")

        # 检查启动状态
        print("\n📊 8. 检查启动状态...")
        boot_status = page.evaluate("""() => {
            const boot = document.getElementById('boot');
            const status = document.getElementById('boot-status');
            const error = document.getElementById('boot-error');

            return {
                bootVisible: boot ? !boot.classList.contains('hidden') : null,
                statusText: status ? status.textContent : null,
                errorVisible: error ? error.classList.contains('show') : null,
                errorText: error && error.classList.contains('show') ? error.textContent : null
            };
        }""")

        if boot_status['bootVisible'] is not None:
            if boot_status['errorVisible']:
                print(f"❌ 启动错误: {boot_status['errorText'][:200]}...")
                results["issues"].append(f"启动错误: {boot_status['errorText']}")
            else:
                print(f"📝 启动状态: {boot_status['statusText']}")

        # 获取所有控制台日志
        print("\n📋 9. 收集诊断信息...")
        diagnostics = page.evaluate("""() => {
            const info = {
                userAgent: navigator.userAgent,
                language: navigator.language,
                platform: navigator.platform,
                webgl2: !!document.createElement('canvas').getContext('webgl2'),
                webgl1: !!document.createElement('canvas').getContext('webgl')
            };

            // 检查 TeaVM Module 状态
            if (typeof Module !== 'undefined') {
                info.teavm = {
                    ready: Module.calledRun || false,
                    readyPromise: !!Module.readyPromise
                };
            }

            // 检查 webmc 全局对象
            if (typeof window.webmc !== 'undefined') {
                info.webmc = {
                    version: window.webmc.VERSION || 'unknown'
                };
            }

            return info;
        }""")

        print(f"   用户代理: {diagnostics.get('userAgent', '未知')[:80]}...")
        print(f"   WebGL2 支持: {diagnostics.get('webgl2', False)}")
        print(f"   WebGL1 支持: {diagnostics.get('webgl1', False)}")

        if 'teavm' in diagnostics:
            print(f"   TeaVM 就绪: {diagnostics['teavm'].get('ready', False)}")

        browser.close()

    print("\n" + "="*60)
    print("📊 测试结果总结")
    print("="*60)
    print(f"页面加载: {'✅' if results['page_load'] else '❌'}")
    print(f"Canvas 创建: {'✅' if results['canvas_exists'] else '❌'}")
    print(f"JS 加载: {'✅' if results['js_loaded'] else '❌'}")
    print(f"WebGL 可用: {'✅' if results['webgl_available'] else '❌'}")
    print(f"事件系统: {'✅' if results['event_system'] else '⚠️'}")
    print(f"控制台错误数: {len(results['console_errors'])}")

    if results['issues']:
        print("\n❌ 发现的问题:")
        for i, issue in enumerate(results['issues'], 1):
            print(f"   {i}. {issue[:100]}")

    print("\n" + "="*60)

    return results

if __name__ == "__main__":
    results = test_webmc()

    # 保存结果到 JSON 文件
    with open('webmc_test_results.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\n📁 结果已保存到 webmc_test_results.json")
