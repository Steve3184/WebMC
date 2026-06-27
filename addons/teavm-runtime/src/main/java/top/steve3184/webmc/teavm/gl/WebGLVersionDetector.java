package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.browser.WindowAnimationFrameCallback;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLContextAttributes;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * WebGL version detection and context acquisition.
 *
 * Supports:
 * - WebGL 2.0 (primary, full features)
 * - WebGL 1.0 (fallback, limited functionality)
 *
 * Detection is performed at startup and the result is cached.
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

    /**
     * Detects the best available WebGL version.
     * First tries WebGL 2.0, then falls back to WebGL 1.0.
     * Result is cached after first call.
     */
    public static WebGLVersion detect() {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        HTMLCanvasElement canvas = (HTMLCanvasElement) Window.current().getDocument().getElementById("game-canvas");
        if (canvas == null) {
            cachedVersion = WebGLVersion.NONE;
            cachedVersionString = "No canvas";
            return cachedVersion;
        }

        // Try WebGL 2.0 first
        WebGL2RenderingContext gl2 = tryGetWebGL2(canvas);
        if (gl2 != null) {
            cachedVersion = WebGLVersion.WEBGL2;
            cachedVersionString = getVersionString(gl2);
            return cachedVersion;
        }

        // Fall back to WebGL 1.0
        WebGLRenderingContext gl1 = tryGetWebGL1(canvas);
        if (gl1 != null) {
            cachedVersion = WebGLVersion.WEBGL1;
            cachedVersionString = getWebGL1VersionString(gl1);
            return cachedVersion;
        }

        cachedVersion = WebGLVersion.NONE;
        cachedVersionString = "WebGL not supported";
        return cachedVersion;
    }

    /**
     * Returns the cached version string (WebGL version + renderer info).
     */
    public static String getVersionString() {
        if (cachedVersionString == null) {
            detect();
        }
        return cachedVersionString;
    }

    /**
     * Checks if WebGL 2.0 is available.
     */
    public static boolean isWebGL2Available() {
        return detect() == WebGLVersion.WEBGL2;
    }

    /**
     * Checks if WebGL 1.0 is available (may have limited functionality).
     */
    public static boolean isWebGL1Available() {
        WebGLVersion v = detect();
        return v == WebGLVersion.WEBGL1;
    }

    /**
     * Checks if any WebGL is available.
     */
    public static boolean isAnyWebGLAvailable() {
        return detect() != WebGLVersion.NONE;
    }

    /**
     * Checks if the detected WebGL version supports the required features.
     * WebGL 1.0 has limited support - many MC features require WebGL 2.0.
     */
    public static boolean supportsRequiredFeatures() {
        return detect() == WebGLVersion.WEBGL2;
    }

    private static WebGL2RenderingContext tryGetWebGL2(HTMLCanvasElement canvas) {
        try {
            WebGLContextAttributes attrs = WebGLContextAttributes.create();
            attrs.setAlpha(false);
            attrs.setAntialias(false);
            attrs.setDepth(true);
            attrs.setStencil(false);
            attrs.setPreserveDrawingBuffer(false);
            attrs.setPowerPreference("default");

            WebGL2RenderingContext ctx = canvas.getContext("webgl2", attrs);
            return ctx;
        } catch (Throwable t) {
            return null;
        }
    }

    private static WebGLRenderingContext tryGetWebGL1(HTMLCanvasElement canvas) {
        try {
            WebGLContextAttributes attrs = WebGLContextAttributes.create();
            attrs.setAlpha(false);
            attrs.setAntialias(false);
            attrs.setDepth(true);
            attrs.setStencil(false);
            attrs.setPreserveDrawingBuffer(false);
            attrs.setPowerPreference("default");

            WebGLRenderingContext ctx = canvas.getContext("webgl", attrs);
            return ctx;
        } catch (Throwable t) {
            return null;
        }
    }

    private static native String getVersionString(WebGL2RenderingContext gl) /*-{
        return gl.getParameter(gl.VERSION) + " / " + gl.getParameter(gl.VENDOR) + " / " + gl.getParameter(gl.RENDERER);
    }-*/;

    private static native String getWebGL1VersionString(WebGLRenderingContext gl) /*-{
        return gl.getParameter(gl.VERSION) + " / " + gl.getParameter(gl.VENDOR) + " / " + gl.getParameter(gl.RENDERER);
    }-*/;
}
