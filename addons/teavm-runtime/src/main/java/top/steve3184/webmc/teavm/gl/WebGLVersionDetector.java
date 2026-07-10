package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * WebGL version detection and context acquisition.
 */
public final class WebGLVersionDetector {

    public enum WebGLVersion {
        WEBGL2,
        WEBGL1,
        NONE
    }

    private static WebGLVersion cachedVersion = null;
    private static String cachedVersionString = null;

    private WebGLVersionDetector() {}

    public static WebGLVersion detect() {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        HTMLCanvasElement canvas = getCanvasElement();
        if (canvas == null) {
            cachedVersion = WebGLVersion.NONE;
            cachedVersionString = "No canvas";
            return cachedVersion;
        }

        // Try WebGL 2.0 first
        JSObject gl2 = tryGetWebGL2(canvas);
        if (gl2 != null) {
            cachedVersion = WebGLVersion.WEBGL2;
            cachedVersionString = getWebGLVersionString(gl2);
            return cachedVersion;
        }

        // Fall back to WebGL 1.0
        JSObject gl1 = tryGetWebGL1(canvas);
        if (gl1 != null) {
            cachedVersion = WebGLVersion.WEBGL1;
            cachedVersionString = getWebGLVersionString(gl1);
            return cachedVersion;
        }

        cachedVersion = WebGLVersion.NONE;
        cachedVersionString = "WebGL not supported";
        return cachedVersion;
    }

    public static String getVersionString() {
        if (cachedVersionString == null) {
            detect();
        }
        return cachedVersionString;
    }

    public static boolean isWebGL2Available() {
        return detect() == WebGLVersion.WEBGL2;
    }

    public static boolean isWebGL1Available() {
        return detect() == WebGLVersion.WEBGL1;
    }

    public static boolean isAnyWebGLAvailable() {
        return detect() != WebGLVersion.NONE;
    }

    public static boolean supportsRequiredFeatures() {
        return detect() == WebGLVersion.WEBGL2;
    }

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (c && c.tagName === 'CANVAS') return c;" +
        "return null;")
    private static native HTMLCanvasElement getCanvasElement();

    @JSBody(params = {"canvas", "type"}, script =
        "var options = {" +
        "  alpha: false," +
        "  antialias: false," +
        "  depth: true," +
        "  stencil: false," +
        "  powerPreference: 'default'" +
        "};" +
        "try {" +
        "  return canvas.getContext(type, options);" +
        "} catch(e) {" +
        "  return null;" +
        "}")
    private static native JSObject getWebGLContext(HTMLCanvasElement canvas, String type);

    private static JSObject tryGetWebGL2(HTMLCanvasElement canvas) {
        return getWebGLContext(canvas, "webgl2");
    }

    private static JSObject tryGetWebGL1(HTMLCanvasElement canvas) {
        return getWebGLContext(canvas, "webgl");
    }

    @JSBody(params = "ctx", script =
        "try {" +
        "  return ctx.getParameter(ctx.VERSION) + ' / ' + ctx.getParameter(ctx.VENDOR) + ' / ' + ctx.getParameter(ctx.RENDERER);" +
        "} catch(e) {" +
        "  return 'Unknown';" +
        "}")
    private static native String getWebGLVersionString(JSObject ctx);
}
