package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLRenderingContext;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.ShaderManager;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * High-performance render engine with GPU-adaptive settings.
 * Manages the rendering pipeline with automatic optimization.
 */
public final class RenderEngine {

    private static RenderEngine instance;

    private WebGLRenderingContext gl;
    private GpuDetector.GpuProfile profile;
    private BatchBuffer batchBuffer;
    private TextureManager textureManager;

    // Canvas dimensions
    private int canvasWidth = 0;
    private int canvasHeight = 0;

    // Render settings
    private boolean vsyncEnabled = true;
    private boolean fogEnabled = true;
    private float fogStart = 0.8f;
    private float fogEnd = 1.0f;
    private float[] fogColor = new float[]{0.55f, 0.71f, 0.89f}; // Sky blue

    // Stats
    private int framesRendered = 0;
    private long lastRenderTime = 0;
    private float averageFrameTime = 16.67f; // Default 60 FPS

    // Frame limiter
    private long targetFrameTime = 16; // ~60 FPS
    private long lastFrameStart = 0;

    private RenderEngine() {
        gl = WebGLContextHolder.gl();
        profile = GpuDetector.getProfile();
        batchBuffer = new BatchBuffer();
        textureManager = TextureManager.getInstance();
    }

    /**
     * Get singleton instance.
     */
    public static RenderEngine getInstance() {
        if (instance == null) {
            instance = new RenderEngine();
        }
        return instance;
    }

    /**
     * Initialize the render engine.
     */
    public void init() {
        if (gl == null) {
            log("[RenderEngine] Cannot init: WebGL not available");
            return;
        }

        batchBuffer.init();
        configureForGpuTier();

        log("[RenderEngine] Initialized for " + profile.getTierName() + " GPU");
    }

    /**
     * Configure render settings based on GPU tier.
     */
    private void configureForGpuTier() {
        switch (profile.tier) {
            case ULTRA:
                targetFrameTime = 8; // 120 FPS target
                fogEnabled = true;
                fogStart = 0.7f;
                fogEnd = 1.0f;
                break;
            case HIGH:
                targetFrameTime = 11; // 90 FPS target
                fogEnabled = true;
                fogStart = 0.75f;
                fogEnd = 1.0f;
                break;
            case MEDIUM:
                targetFrameTime = 16; // 60 FPS target
                fogEnabled = true;
                fogStart = 0.6f;
                fogEnd = 0.9f;
                break;
            case LOW:
                targetFrameTime = 33; // 30 FPS target
                fogEnabled = true;
                fogStart = 0.5f;
                fogEnd = 0.8f;
                break;
            default:
                targetFrameTime = 50; // 20 FPS target
                fogEnabled = true;
                fogStart = 0.4f;
                fogEnd = 0.7f;
        }
    }

    /**
     * Begin frame.
     */
    public void beginFrame() {
        if (gl == null) return;

        lastFrameStart = System.currentTimeMillis();

        // Clear buffers
        gl.clearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT |
                 WebGLRenderingContext.DEPTH_BUFFER_BIT);
    }

    /**
     * End frame and handle vsync/frame limiting.
     */
    public void endFrame() {
        if (gl == null) return;

        framesRendered++;

        // Calculate frame time
        long frameTime = System.currentTimeMillis() - lastFrameStart;
        averageFrameTime = 0.9f * averageFrameTime + 0.1f * frameTime;

        // Frame limiting
        if (vsyncEnabled) {
            // Simple frame limiting
            long elapsed = System.currentTimeMillis() - lastFrameStart;
            long sleepTime = targetFrameTime - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Update FPS tracking
        WebGLContextHolder.updateFPS();
    }

    /**
     * Set viewport for 3D rendering.
     */
    public void set3DViewport() {
        if (gl == null) return;
        gl.viewport(0, 0, canvasWidth, canvasHeight);
        gl.enable(WebGLRenderingContext.DEPTH_TEST);
        gl.depthFunc(WebGLRenderingContext.LEQUAL);
    }

    /**
     * Set viewport for 2D UI rendering.
     */
    public void set2DViewport() {
        if (gl == null) return;
        gl.viewport(0, 0, canvasWidth, canvasHeight);
        gl.disable(WebGLRenderingContext.DEPTH_TEST);
    }

    /**
     * Resize canvas.
     */
    public void resize(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        if (gl != null) {
            gl.viewport(0, 0, width, height);
        }
        log("[RenderEngine] Resized to " + width + "x" + height);
    }

    /**
     * Get canvas width.
     */
    public int getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * Get canvas height.
     */
    public int getCanvasHeight() {
        return canvasHeight;
    }

    /**
     * Set fog enabled.
     */
    public void setFogEnabled(boolean enabled) {
        fogEnabled = enabled;
    }

    /**
     * Check if fog is enabled.
     */
    public boolean isFogEnabled() {
        return fogEnabled;
    }

    /**
     * Set fog parameters.
     */
    public void setFog(float start, float end, float[] color) {
        fogStart = start;
        fogEnd = end;
        if (color != null && color.length >= 3) {
            fogColor = color;
        }
    }

    /**
     * Get fog start distance.
     */
    public float getFogStart() {
        return fogStart;
    }

    /**
     * Get fog end distance.
     */
    public float getFogEnd() {
        return fogEnd;
    }

    /**
     * Get fog color.
     */
    public float[] getFogColor() {
        return fogColor;
    }

    /**
     * Set target frame time.
     */
    public void setTargetFPS(int fps) {
        if (fps <= 0) {
            vsyncEnabled = false;
        } else {
            targetFrameTime = 1000 / fps;
            vsyncEnabled = true;
        }
    }

    /**
     * Get current FPS.
     */
    public float getCurrentFPS() {
        return WebGLContextHolder.getFPS();
    }

    /**
     * Get average frame time in ms.
     */
    public float getAverageFrameTime() {
        return averageFrameTime;
    }

    /**
     * Get GPU tier.
     */
    public String getGpuTier() {
        return profile.getTierName();
    }

    /**
     * Get batch buffer for custom rendering.
     */
    public BatchBuffer getBatchBuffer() {
        return batchBuffer;
    }

    /**
     * Get texture manager.
     */
    public TextureManager getTextureManager() {
        return textureManager;
    }

    /**
     * Check if using WebGL2.
     */
    public boolean isWebGL2() {
        return gl instanceof WebGL2RenderingContext;
    }

    /**
     * Get WebGL context.
     */
    public WebGLRenderingContext getGL() {
        return gl;
    }

    /**
     * Get frames rendered.
     */
    public int getFramesRendered() {
        return framesRendered;
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
