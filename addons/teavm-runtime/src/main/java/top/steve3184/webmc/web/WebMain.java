package top.steve3184.webmc.web;

import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;
import top.steve3184.webmc.teavm.gl.WebGLVersionDetector;

/**
 * Phase-2 hello-world entry. Lives in the main source set (blaze3d-impl addon)
 * so the TeaVM Gradle plugin picks it up without extra sourceSet plumbing.
 *
 * Goal: prove the TeaVM toolchain end-to-end.
 *   - Compile to game.js
 *   - Browser loads game.js, calls main()
 *   - main() acquires the WebGL2 context on #canvas, clears to green, logs the version
 *
 * Does NOT call into net.minecraft.* — TeaVM's reachability analysis stops here,
 * so the output JS is small (~tens of KB) instead of pulling in MC's 6000-class graph.
 *
 * The {@code @JSBody} annotation lets us drop raw JS inline; no teavm-jso-apis
 * dependency needed for this minimal demo.
 */
public final class WebMain {
    @JSBody(params = "msg", script = "console.log('[mc-web] ' + msg);")
    private static native void log(String msg);

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (!c) return null;" +
        "return c.getContext('webgl2', { alpha: false, antialias: false, depth: true, stencil: false, preserveDrawingBuffer: false });")
    private static native WebGL2RenderingContext acquireGL();

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (!c) return null;" +
        "return c.getContext('webgl', { alpha: false, antialias: false, depth: true, stencil: false, preserveDrawingBuffer: false });")
    private static native org.teavm.jso.webgl.WebGLRenderingContext acquireGL1();

    @JSBody(params = "g", script =
        "if (!g) return 'no webgl2';" +
        "g.clearColor(0.2, 0.2, 0.2, 1.0);" +
        "g.clear(g.COLOR_BUFFER_BIT);" +
        "return g.getParameter(g.VERSION) + ' / ' + g.getParameter(g.VENDOR) + ' / ' + g.getParameter(g.RENDERER);")
    private static native String describeGL(WebGL2RenderingContext g);

    @JSBody(params = "g", script =
        "var vao = g.createVertexArray();" +
        "g.bindVertexArray(vao);")
    private static native void bindDefaultVao(WebGL2RenderingContext g);

    @JSBody(params = "id", script =
        "var el = document.getElementById(id);" +
        "if (el) el.classList.add('hidden');")
    private static native void hide(String id);

    @JSBody(params = {"id", "text"}, script =
        "var el = document.getElementById(id);" +
        "if (el) el.textContent = text;")
    private static native void setText(String id, String text);

    @JSBody(params = {"name", "detail"}, script =
        "try {" +
        "  if (typeof window !== 'undefined' && typeof window.__webmcStartupMark === 'function') {" +
        "    window.__webmcStartupMark(String(name || ''), String(detail || ''));" +
        "  }" +
        "} catch (e) {}")
    private static native void startupMark(String name, String detail);

    @JSBody(script =
        "try {" +
        "  var v = window.webmcBootMode;" +
        "  return v == null ? 'webSafeBoot' : String(v);" +
        "} catch (e) { return 'webSafeBoot'; }")
    private static native String bootMode();

    public static void main(String[] args) {
        // mc-web: silence SLF4J's own init warnings ("No SLF4J providers...",
        // "Defaulting to NOP", "See ...") and route anything it still emits to
        // stdout. TeaVM's ServiceLoader can't discover META-INF/services so
        // SLF4J always falls back to NOP; MC code goes through our shadow
        // LogUtils → ConsoleLogger instead, so SLF4J's internal reporter is
        // pure noise. Must be set BEFORE any class that touches SLF4J (e.g.
        // SoundEngine's clinit calls MarkerFactory.getMarker, which triggers
        // LoggerFactory.bind and Reporter.<clinit>).
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        System.setProperty("slf4j.internal.report.stream", "System.out");
        WebFilteredPrintStream.install();
        startupMark("webmain:entered", "args=" + (args == null ? 0 : args.length));

        // Boot the browser-side filesystem before ANY other MC or JDK NIO code runs.
        try {
            startupMark("webmain:vfs-boot:begin", "");
            top.steve3184.webmc.vfs.WebFs.boot();
            startupMark("webmain:vfs-boot:done", "");
            startupMark("webmain:vfs-preload:begin", "game.vfs");
            top.steve3184.webmc.vfs.WebFs.preload("game.vfs");
            startupMark("webmain:vfs-preload:done", "game.vfs");
        } catch (Throwable t) {
            startupMark("webmain:vfs:failed", String.valueOf(t));
            log("WebFs init failed (non-fatal): " + t);
        }

        log("WebMain.main start (args=" + args.length + ")");

        // Detect WebGL version and acquire appropriate context
        WebGLVersionDetector.WebGLVersion version = WebGLVersionDetector.detect();
        log("WebGL version detected: " + version);

        WebGL2RenderingContext gl = null;
        String info = null;

        if (version == WebGLVersionDetector.WebGLVersion.WEBGL2) {
            gl = acquireGL();
            if (gl != null) {
                WebGLContextHolder.install(gl);
                bindDefaultVao(gl);
                info = describeGL(gl);
                startupMark("webmain:webgl:done", info);
                log("WebGL2 init: " + info);
                setText("status", "WebGL2 OK: " + info);
            } else {
                log("WebGL2 context acquisition failed after detection succeeded");
                version = WebGLVersionDetector.WebGLVersion.NONE;
            }
        } else if (version == WebGLVersionDetector.WebGLVersion.WEBGL1) {
            // WebGL 1.0 fallback mode - limited functionality
            WebGLContextHolder.installWebGL1Fallback();
            info = WebGLVersionDetector.getVersionString();
            startupMark("webmain:webgl:fallback", info);
            log("WebGL1 fallback mode: " + info);
            log("WARNING: WebGL 1.0 detected - Minecraft rendering will be limited");
            setText("status", "WebGL1 (limited): " + info);
            // Continue with MC main in fallback mode - some features will be disabled
        } else {
            log("no canvas/webgl — abort");
            setText("status", "WebGL not available");
            return;
        }

        hide("progress");

        // Sanity check: confirm System.err reaches DevTools.
        System.err.println("[stderr-test] before MC main");
        System.out.println("[stdout-test] before MC main");

        // Install the pure-Java PNG decoder (synchronous; replaces the old
        // Canvas-based decoder that stalled resource reload via Thread.sleep).
        try {
            top.steve3184.webmc.teavm.io.ImageDecodeBackendHolder.install(
                new top.steve3184.webmc.teavm.io.TinyPngDecoder());
            log("TinyPngDecoder installed");
        } catch (Throwable t) {
            log("ImageDecodeBackend install failed: " + t);
        }

        // Install LWJGL/GLFW backend stubs before MC's Window code runs.
        try {
            top.steve3184.webmc.teavm.glfw.WindowBackendHolder.install(
                new top.steve3184.webmc.teavm.runtime.CanvasWindowBackend());
            log("CanvasWindowBackend installed");
        } catch (Throwable t) {
            log("WindowBackend install failed: " + t);
        }

        // MC main probe.
        try {
            org.slf4j.Logger probe = com.mojang.logging.LogUtils.getLogger();
            log("LogUtils returned: " + probe.getClass().getName());
            probe.info("probe.info() works");

            String mode = bootMode();
            log("boot mode: " + mode);
            if ("mcMain".equals(mode)) {
                System.setProperty("webmc.forceFullMain", "true");
                if (args == null || args.length == 0) {
                    args = new String[] {
                        "--username", "WebPlayer",
                        "--version", "1.21.8",
                        "--gameDir", ".",
                        "--assetsDir", "/assets",
                        "--assetIndex", "26",
                        "--accessToken", "0",
                        "--userType", "legacy",
                        "--versionType", "release"
                    };
                }
                log("MC main call begin");
                startupMark("webmain:mc-main:begin", "args=" + args.length);
                net.minecraft.client.main.Main.main(args);
                startupMark("webmain:mc-main:end", "");
                log("MC main call end");
                log("MC main returned normally");
            } else {
                startupMark("webmain:mc-main:skipped", mode);
                log("MC main skipped in webSafeBoot mode");
            }
        } catch (Throwable t) {
            startupMark("webmain:mc-main:threw", String.valueOf(t));
            log("MC main threw: " + t);
            try {
                System.out.println(CrashReport.forThrowable(t, "WebMain top-level catch").getFriendlyReport(ReportType.CRASH));
            } catch (Throwable reportThrowable) {
                log("Failed to print crash report: " + reportThrowable);
            }
            t.printStackTrace();
        }
    }

    private WebMain() {}
}
