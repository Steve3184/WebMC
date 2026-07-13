package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * Central WebGL context manager with performance optimization.
 * Handles context creation, loss recovery, and resource management.
 */
public final class WebGLContextHolder {

    private static WebGLRenderingContext gl;
    private static HTMLCanvasElement canvas;
    private static boolean initialized = false;
    private static int contextVersion = 0;

    // Performance tracking
    private static int frames = 0;
    private static long lastTime = 0;
    private static float fps = 0;

    private WebGLContextHolder() {}

    /**
     * Initialize WebGL context with optimal settings.
     */
    public static void init(HTMLCanvasElement canvasEl) {
        if (initialized && gl != null) return;

        canvas = canvasEl;

        // Try WebGL2 first
        gl = (WebGLRenderingContext) canvasEl.getContext("webgl2");
        if (gl != null) {
            contextVersion = 2;
            log("[WebGL] Using WebGL2 context");
        } else {
            // Fall back to WebGL 1.0
            gl = (WebGLRenderingContext) canvasEl.getContext("webgl");
            if (gl != null) {
                contextVersion = 1;
                log("[WebGL] Using WebGL1 context");
            }
        }

        if (gl == null) {
            gl = (WebGLRenderingContext) canvasEl.getContext("experimental-webgl");
            if (gl != null) {
                contextVersion = 1;
                log("[WebGL] Using experimental-webgl context");
            }
        }

        if (gl == null) {
            log("[WebGL] ERROR: No WebGL context available!");
            return;
        }

        // Get GPU info
        String vendor = getParameterString(WebGLRenderingContext.VENDOR);
        String renderer = getParameterString(WebGLRenderingContext.RENDERER);
        log("[WebGL] Vendor: " + vendor + ", Renderer: " + renderer);
        log("[WebGL] Version: " + contextVersion);

        // Apply performance optimizations
        applyPerformanceSettings();

        // Set up context loss handling
        setupContextLossHandling(canvasEl);

        initialized = true;
        lastTime = System.currentTimeMillis();
    }

    /**
     * Get WebGL parameter as String.
     */
    private static String getParameterString(int pname) {
        return getStringFromGL(gl, pname);
    }

    @JSBody(params = {"gl", "pname"}, script =
        "try { var v = gl.getParameter(pname); return (v === undefined || v === null) ? '' : String(v); } catch(e) { return ''; }"
    )
    private static native String getStringFromGL(WebGLRenderingContext gl, int pname);

    /**
     * Get the WebGL context.
     */
    public static WebGLRenderingContext gl() {
        return gl;
    }

    /**
     * Get the canvas element.
     */
    public static HTMLCanvasElement canvas() {
        return canvas;
    }

    /**
     * Check if initialized.
     */
    public static boolean isInitialized() {
        return initialized && gl != null;
    }

    /**
     * Get WebGL version (1 or 2).
     */
    public static int getVersion() {
        return contextVersion;
    }

    /**
     * Check if using WebGL2.
     */
    public static boolean isWebGL2() {
        return contextVersion >= 2;
    }

    /**
     * Apply performance-optimized settings.
     */
    private static void applyPerformanceSettings() {
        if (gl == null) return;

        // Disable depth testing by default (2D rendering)
        gl.disable(WebGLRenderingContext.DEPTH_TEST);

        // Enable blending for transparency
        gl.enable(WebGLRenderingContext.BLEND);
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

        // Backface culling for 3D
        gl.enable(WebGLRenderingContext.CULL_FACE);
        gl.cullFace(WebGLRenderingContext.BACK);
    }

    /**
     * Set up context loss/restore event handling.
     */
    private static void setupContextLossHandling(HTMLCanvasElement canvasEl) {
        // Register webglcontextlost event handler
        canvasEl.addEventListener("webglcontextlost", (event) -> {
            event.preventDefault();
            log("[WebGL] Context lost!");
            initialized = false;
        });

        // Register webglcontextrestored event handler
        canvasEl.addEventListener("webglcontextrestored", (event) -> {
            log("[WebGL] Context restored!");
            initialized = true;
        });
    }

    /**
     * Update FPS counter.
     */
    public static void updateFPS() {
        frames++;
        long now = System.currentTimeMillis();
        if (now - lastTime >= 1000) {
            fps = frames * 1000f / (now - lastTime);
            frames = 0;
            lastTime = now;
        }
    }

    /**
     * Get current FPS.
     */
    public static float getFPS() {
        return fps;
    }

    /**
     * Install WebGL context (called from WebMain).
     */
    public static void install(WebGLRenderingContext ctx) {
        if (initialized && gl != null) return;
        gl = ctx;
        initialized = true;
        lastTime = System.currentTimeMillis();
        applyPerformanceSettings();
    }

    /**
     * Install WebGL1 fallback context.
     */
    public static void installWebGL1Fallback() {
        if (initialized && gl != null) return;
        log("[WebGL] Using WebGL1 fallback mode");
        initialized = true;
        lastTime = System.currentTimeMillis();
    }

    /**
     * Get WebGL parameter as int.
     */
    public static int getParameterInt(int pname) {
        if (gl == null) return 0;
        return getIntFromGL(gl, pname);
    }

    @JSBody(params = {"gl", "pname"}, script =
        "try { var v = gl.getParameter(pname); return (v === undefined || v === null) ? 0 : (v | 0); } catch(e) { return 0; }"
    )
    private static native int getIntFromGL(WebGLRenderingContext gl, int pname);

    /**
     * Get WebGL info.
     */
    public static WebGLInfo getInfo() {
        if (gl == null) return null;
        return new WebGLInfo(
            getParameterInt(WebGLRenderingContext.MAX_TEXTURE_SIZE),
            getParameterInt(WebGLRenderingContext.MAX_VERTEX_ATTRIBS),
            getParameterInt(WebGLRenderingContext.MAX_VARYING_VECTORS),
            getParameterInt(WebGLRenderingContext.MAX_TEXTURE_IMAGE_UNITS),
            contextVersion
        );
    }

    // Info holder
    public static class WebGLInfo {
        public final int maxTextureSize;
        public final int maxVertexAttribs;
        public final int maxVaryingVectors;
        public final int maxTextureUnits;
        public final int version;

        public WebGLInfo(int maxTextureSize, int maxVertexAttribs, int maxVaryingVectors,
                        int maxTextureUnits, int version) {
            this.maxTextureSize = maxTextureSize;
            this.maxVertexAttribs = maxVertexAttribs;
            this.maxVaryingVectors = maxVaryingVectors;
            this.maxTextureUnits = maxTextureUnits;
            this.version = version;
        }
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
