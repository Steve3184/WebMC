package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.webgl.WebGL2RenderingContext;

/**
 * Holds the live WebGL2 rendering context for the canvas. Set once by
 * {@code WebMain} during boot, read by the GPU layer (WebGpuBuffer,
 * WebGpuTexture, WebRenderPass, etc.) to issue real GL calls.
 *
 * Decoupled from the {@code GLBackendHolder} integer-handle abstraction —
 * MC's modern Mojang 1.21 API works directly with high-level
 * GpuBuffer/Texture/RenderPass, so we route those straight to GL through
 * the JSO context rather than through the legacy GLBackend interface.
 *
 * Note: WebGL 1.0 fallback is detected but MC 1.21.8 requires WebGL 2.0
 * for its shader features (UBOs, texture arrays, etc.). WebGL 1.0 only
 * provides a graceful degradation path with limited rendering.
 */
public final class WebGLContextHolder {
    private static WebGL2RenderingContext gl;
    private static boolean webgl1Fallback = false;

    private WebGLContextHolder() {}

    /**
     * Install the WebGL 2.0 context.
     * @throws IllegalStateException if a context is already installed
     */
    public static void install(WebGL2RenderingContext context) {
        if (gl != null) {
            throw new IllegalStateException("WebGL2 context already installed");
        }
        gl = context;
        webgl1Fallback = false;
    }

    /**
     * Install in WebGL 1.0 fallback mode.
     * This indicates limited functionality is available.
     */
    public static void installWebGL1Fallback() {
        webgl1Fallback = true;
        // In fallback mode, we cannot install a WebGL 2.0 context
        // The rendering pipeline will need to handle this gracefully
    }

    public static WebGL2RenderingContext gl() {
        if (gl == null) {
            throw new IllegalStateException(
                "WebGL2 context not installed. WebMain must call WebGLContextHolder.install(...) before MC main.");
        }
        return gl;
    }

    public static boolean isInstalled() {
        return gl != null || webgl1Fallback;
    }

    /**
     * Returns true if running in WebGL 1.0 fallback mode.
     * In this mode, some advanced rendering features are disabled.
     */
    public static boolean isWebGL1Fallback() {
        return webgl1Fallback;
    }

    /**
     * Returns true if WebGL 2.0 context is available.
     */
    public static boolean hasWebGL2() {
        return gl != null;
    }
}
