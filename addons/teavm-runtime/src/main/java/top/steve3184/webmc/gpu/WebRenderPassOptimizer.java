package top.steve3184.webmc.gpu;

import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * Optimizer that adds performance optimizations to the rendering pipeline.
 * - State change batching
 * - Draw call optimization
 * - Performance statistics
 */
public final class WebRenderPassOptimizer {

    private static WebGLStateCache stateCache;
    private static RenderStats stats;
    private static boolean enabled = false;

    private WebRenderPassOptimizer() {}

    /**
     * Initialize the optimizer. Called once during WebMain setup.
     */
    public static void init() {
        if (enabled) return;

        stateCache = new WebGLStateCache();
        stats = RenderStats.getInstance();
        enabled = true;

        log("[mc-web/optimizer] GPU Profile: " + GpuDetector.getProfile());
        log("[mc-web/optimizer] Optimizer initialized");
    }

    /**
     * Check if optimizer is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the state cache for direct use.
     */
    public static WebGLStateCache getStateCache() {
        return stateCache;
    }

    /**
     * Get the render statistics.
     */
    public static RenderStats getStats() {
        return stats;
    }

    /**
     * Get GPU profile.
     */
    public static GpuProfile getGpuProfile() {
        return GpuDetector.getProfile();
    }

    /**
     * Called at the start of each frame.
     */
    public static void onFrameStart() {
        if (!enabled || stats == null) return;
        stats.onFrameStart();
    }

    /**
     * Called at the end of each frame.
     */
    public static void onFrameEnd() {
        if (!enabled || stats == null) return;
        stats.onFrameEnd();
    }

    /**
     * Get current FPS.
     */
    public static int getCurrentFps() {
        if (stats == null) return 0;
        return stats.getCurrentFps();
    }

    /**
     * Get current frame time in milliseconds.
     */
    public static double getFrameTime() {
        if (stats == null) return 0;
        return stats.getFrameTime();
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
