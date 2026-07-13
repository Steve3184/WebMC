"""
WebMC 诊断脚本 - 检查核心加载问题
"""
from playwright.sync_api import sync_playwright
import time
import sys

def diagnose_webmc():
    print("="*70)
    print("WebMC 诊断 - 检查核心加载问题")
    print("="*70)

    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=['--enable-webgl', '--use-gl=swiftshader']
        )
        page = browser.new_page()

        # 监听控制台
        console_log = []
        def handle_console(msg):
            console_log.append({'type': msg.type, 'text': msg.text})

        page.on('console', handle_console)

        # 监听页面错误
        page_errors = []
        def handle_error(error):
            page_errors.append(str(error))

        page.on('pageerror', handle_error)

        print("\n📄 1. 加载 index.html...")
        try:
            page.goto('http://localhost:8080/index.html', timeout=60000)
            print("✅ 页面加载成功")
        except Exception as e:
            print(f"❌ 页面加载失败: {e}")
            browser.close()
            return

        # 等待页面完全加载
        print("\n⏳ 2. 等待页面加载完成...")
        page.wait_for_load_state('networkidle', timeout=60000)
        print("✅ 页面网络空闲")

        # 检查关键脚本是否加载
        print("\n📜 3. 检查关键脚本是否加载:")

        # 检查所有脚本标签
        script_tags = page.query_selector_all('script[src]')
        print(f"   总共 {len(script_tags)} 个脚本标签")

        # 检查每个脚本的加载状态
        for i, script in enumerate(script_tags, 1):
            src = script.get_attribute('src')
            if not src:
                continue

            # 检查是否已加载
            loaded = page.evaluate("""(src) => {
                const script = document.querySelector(`script[src='${src}']`);
                if (!script) return false;
                return script.complete || script.readyState === 'complete' || script.readyState === 'loaded';
            }""", src)

            filename = src.split('/')[-1]
            status = "✅" if loaded else "❌"
            print(f"   {status} {i}. {filename}")

        # 检查全局对象
        print("\n🔍 4. 检查全局 JavaScript 对象:")

        # 检查 WebMC 核心对象
        core_objects = [
            'vfs', 'socket', 'performance', 'performanceHUD', 'bootstrap', 'game',
            'Module', 'teavm', 'webmc', 'webmcPerf', 'webmcHUD', 'webmcInput',
            'GLFW', 'webmcReady'
        ]

        for obj in core_objects:
            exists = page.evaluate(f"""() => {{
                return typeof {obj} !== 'undefined';
            }}""")
            status = "✅" if exists else "❌"
            print(f"   {status} {obj}")

        # 检查 TeaVM Module 状态
        print("\n⚙️ 5. 检查 TeaVM Module 状态:")
        module_info = page.evaluate("""() => {
            if (typeof Module === 'undefined') return { exists: false };

            return {
                exists: true,
                calledRun: Module.calledRun || false,
                calledMain: Module.calledMain || false,
                ready: Module.ready || false,
                totalDependencies: Module.totalDependencies || 0,
                dependenciesFulfilled: Module.dependenciesFulfilled || {},
                memorySize: Module.HEAP ? Module.HEAP.length : 0
            };
        }""")

        if module_info.get('exists'):
            print(f"   ✅ Module 存在")
            print(f"   calledRun: {module_info.get('calledRun')}")
            print(f"   calledMain: {module_info.get('calledMain')}")
            print(f"   ready: {module_info.get('ready')}")
            print(f"   totalDependencies: {module_info.get('totalDependencies')}")
            print(f"   memorySize: {module_info.get('memorySize')} bytes")
        else:
            print(f"   ❌ Module 不存在")

        # 检查控制台错误
        if console_log:
            print("\n📋 6. 控制台日志:")
            for log in console_log[:10]:
                print(f"   [{log['type']}] {log['text'][:100]}...")

        # 检查页面错误
        if page_errors:
            print("\n❌ 7. 页面错误:")
            for err in page_errors[:5]:
                print(f"   {err[:100]}...")

        # 检查启动状态
        print("\n📊 8. 检查启动状态:")
        boot_status = page.evaluate("""() => {
            const boot = document.getElementById('boot');
            const status = document.getElementById('boot-status');
            const error = document.getElementById('boot-error');

            if (!boot) return { visible: false };

            return {
                visible: !boot.classList.contains('hidden'),
                statusText: status ? status.textContent : null,
                errorVisible: error ? error.classList.contains('show') : false,
                errorText: error && error.classList.contains('show') ? error.textContent : null
            };
        }""")

        if boot_status['visible']:
            print(f"   启动状态: {boot_status['statusText']}")
            if boot_status['errorVisible']:
                print(f"   ❌ 启动错误: {boot_status['errorText'][:100]}...")

        # 检查 Canvas
        print("\n🎨 9. 检查 Canvas:")
        canvas = page.query_selector('#game-canvas')
        if canvas:
            width = canvas.get_attribute('width')
            height = canvas.get_attribute('height')
            print(f"   ✅ Canvas 存在: {width}x{height}")

            # 检查 WebGL 上下文
            webgl_context = page.evaluate("""() => {
                const canvas = document.getElementById('game-canvas');
                return canvas ? canvas.getContext('webgl2') || canvas.getContext('webgl') : null;
            }""")
            if webgl_context:
                print(f"   ✅ WebGL 上下文可用")
            else:
                print(f"   ❌ WebGL 上下文不可用")
        else:
            print(f"   ❌ Canvas 不存在")

        # 检查事件系统
        print("\n🎮 10. 检查事件系统:")

        # 检查是否注册了事件监听器
        event_listeners = page.evaluate("""() => {
            const canvas = document.getElementById('game-canvas');
            if (!canvas) return { hasListeners: false };

            const events = ['mousedown', 'mousemove', 'mouseup', 'keydown', 'keyup'];
            const listeners = {};

            events.forEach(event => {
                listeners[event] = canvas.hasAttribute('on' + event) ||
                    window.getComputedStyle(canvas).getPropertyValue('pointer-events') !== 'none';
            });

            return {
                hasListeners: Object.values(listeners).some(v => v),
                listeners: listeners
            };
        }""")

        if event_listeners['hasListeners']:
            print(f"   ✅ 事件监听器存在")
        else:
            print(f"   ❌ 事件监听器不存在")

        # 截图
        print("\n📸 11. 截图...")
        page.screenshot(path='webmc_diagnosis.png', full_page=True)
        print("   已保存到 webmc_diagnosis.png")

        browser.close()

    print("\n" + "="*70)
    print("诊断完成")
    print("="*70)

if __name__ == "__main__":
    # 设置 UTF-8 编码
    sys.stdout.reconfigure(encoding='utf-8')
    diagnose_webmc()
