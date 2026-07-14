"""
WebMC 简单检查脚本 - 检查 TeaVM 是否真正运行
"""
from playwright.sync_api import sync_playwright
import sys

def check_webmc():
    print("="*60)
    print("WebMC TeaVM 运行状态检查")
    print("="*60)

    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=['--enable-webgl', '--use-gl=swiftshader']
        )
        page = browser.new_page()

        # 监听控制台
        console_messages = []

        def handle_console(msg):
            console_messages.append({'type': msg.type, 'text': msg.text})

        page.on('console', handle_console)

        print("\n📄 加载 index.html...")
        page.goto('http://localhost:8080/index.html', timeout=60000)
        page.wait_for_load_state('networkidle', timeout=60000)

        # 等待 TeaVM 初始化
        print("⏳ 等待 TeaVM 初始化 (10秒)...")
        page.wait_for_timeout(10000)

        # 检查 TeaVM 核心对象
        print("\n🔍 检查 TeaVM Java 对象:")

        teavm_objects = [
            'main',           # TeaVM main 函数
            'com',            # Java 包命名空间
            'java',           # Java 运行时
            'top',            # top.steve3184.webmc 包
            'org',            # org.lwjgl 包
            'net',            # net.minecraft 包
            'io',             # io 包
            'mc',             # mc 命名空间
        ]

        for obj in teavm_objects:
            exists = page.evaluate(f"""() => {{
                try {{
                    return typeof {obj} !== 'undefined';
                }} catch (e) {{
                    return false;
                }}
            }}""")
            status = "✅" if exists else "❌"
            print(f"   {status} {obj}")

        # 检查 WebMC 特有对象
        print("\n🔍 检查 WebMC 特有对象:")

        webmc_objects = [
            'top.steve3184',
            'top.steve3184.webmc',
            'top.steve3184.webmc.web',
            'top.steve3184.webmc.teavm',
        ]

        for obj in webmc_objects:
            exists = page.evaluate(f"""() => {{
                try {{
                    return typeof {obj} !== 'undefined';
                }} catch (e) {{
                    return false;
                }}
            }}""")
            status = "✅" if exists else "❌"
            print(f"   {status} {obj}")

        # 检查 Java 运行时类
        print("\n🔍 检查 Java 运行时类:")

        java_classes = [
            'top.steve3184.webmc.web.WebMain',
            'top.steve3184.webmc.web.WebHttp',
        ]

        for cls in java_classes:
            exists = page.evaluate(f"""() => {{
                try {{
                    return typeof {cls} !== 'undefined' && {cls} !== null;
                }} catch (e) {{
                    return false;
                }}
            }}""")
            status = "✅" if exists else "❌"
            print(f"   {status} {cls}")

        # 检查 WebGL 相关对象
        print("\n🔍 检查 WebGL 相关对象:")

        webgl_objects = [
            'top.steve3184.webmc.teavm.gl.GpuDetector',
            'top.steve3184.webmc.teavm.gl.WebGLContextHolder',
            'org.lwjgl.glfw.GLFW',
        ]

        for obj in webgl_objects:
            exists = page.evaluate(f"""() => {{
                try {{
                    return typeof {obj} !== 'undefined' && {obj} !== null;
                }} catch (e) {{
                    return false;
                }}
            }}""")
            status = "✅" if exists else "❌"
            print(f"   {status} {obj}")

        # 检查 JavaScript 命名空间
        print("\n🔍 检查 JavaScript 命名空间:")

        js_namespaces = [
            'vfs',
            'socket',
            'Module',
            'performance',
        ]

        for obj in js_namespaces:
            exists = page.evaluate(f"""() => typeof {obj} !== 'undefined'""")
            status = "✅" if exists else "❌"
            print(f"   {status} {obj}")

        # 打印相关控制台日志
        print("\n📋 控制台日志 (过滤后):")

        important_prefixes = [
            'WebMain', 'WebMC', '[INFO]', '[WARN]', '[ERROR]', 'WebGL',
            'main', 'Module', 'TeaVM', 'GLFW'
        ]

        for msg in console_messages:
            text = msg['text']
            if any(prefix.lower() in text.lower() for prefix in important_prefixes):
                print(f"   [{msg['type']}] {text[:100]}")

        # 检查 Canvas
        print("\n🎨 Canvas 状态:")
        canvas_info = page.evaluate("""() => {
            const canvas = document.getElementById('game-canvas');
            if (!canvas) return { exists: false };

            // Try to get WebGL context
            const gl2 = canvas.getContext('webgl2');
            const gl1 = canvas.getContext('webgl');

            return {
                exists: true,
                width: canvas.width,
                height: canvas.height,
                hasWebGL2: !!gl2,
                hasWebGL1: !!gl1
            };
        }""")

        if canvas_info['exists']:
            print(f"   ✅ Canvas: {canvas_info['width']}x{canvas_info['height']}")
            print(f"   {'✅' if canvas_info['hasWebGL2'] else '❌'} WebGL2: {canvas_info['hasWebGL2']}")
            print(f"   {'✅' if canvas_info['hasWebGL1'] else '❌'} WebGL1: {canvas_info['hasWebGL1']}")

        # 截图
        print("\n📸 截图...")
        page.screenshot(path='webmc_teavm_check.png')
        print("   保存到 webmc_teavm_check.png")

        browser.close()

    print("\n" + "="*60)
    print("检查完成")
    print("="*60)

if __name__ == "__main__":
    sys.stdout.reconfigure(encoding='utf-8')
    check_webmc()
