package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.systems.RenderPass;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * Optimizer that wraps WebRenderPass and adds performance optimizations.
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

        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        stateCache = new WebGLStateCache(gl);
        stats = RenderStats.getInstance();
        stats.enable();
        enabled = true;

        System.out.println("[mc-web/optimizer] GPU Profile: " + GpuDetector.getProfile());
        System.out.println("[mc-web/optimizer] Optimizer initialized");
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
     * Record a draw call.
     */
    public static void onDrawCall(int vertexCount, int indexCount) {
        if (!enabled || stats == null) return;
        stats.onDrawCall(vertexCount, indexCount);
    }

    /**
     * Apply optimized render state.
     */
    public static void applyState(WebGLStateCache.RenderState state) {
        if (!enabled || stateCache == null) return;
        stateCache.applyRenderState(state);
    }

    /**
     * Get current FPS.
     */
    public static int getFps() {
        return stats != null ? stats.getCurrentFps() : 0;
    }

    /**
     * Get average frame time in milliseconds.
     */
    public static float getAverageFrameTime() {
        return stats != null ? stats.getAverageFrameTime() : 0f;
    }

    /**
     * Get diagnostic summary string.
     */
    public static String getSummary() {
        if (stats == null) return "Optimizer not initialized";
        return stats.getSummary();
    }

    /**
     * Log current performance stats.
     */
    public static void logStats() {
        if (stats == null) return;
        System.out.println("[mc-web/perf] " + stats.getSummary());
    }
}
