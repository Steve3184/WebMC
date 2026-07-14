package top.steve3184.webmc.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.browser.Performance;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import top.steve3184.webmc.teavm.WebLog;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.ShaderManager;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;
import top.steve3184.webmc.teavm.gl.WebGLStateCache;
import top.steve3184.webmc.teavm.gl.WebGLVersionDetector;
import top.steve3184.webmc.teavm.gl.render.AdaptiveRenderDistance;
import top.steve3184.webmc.teavm.gl.render.BatchBuffer;
import top.steve3184.webmc.teavm.gl.render.RenderEngine;
import top.steve3184.webmc.teavm.gl.render.RenderStats;
import top.steve3184.webmc.teavm.gl.render.TextureManager;
import top.steve3184.webmc.teavm.runtime.CanvasWindowBackend;
import top.steve3184.webmc.vfs.WebFs;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class WebMain {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static HTMLCanvasElement canvas;
    private static RenderEngine renderEngine;
    private static boolean initialized = false;

    // Performance tracking
    private static int frameCount = 0;
    private static long fpsUpdateTime = 0;
    private static int currentFps = 0;
    private static long lastFrameTime = 0;

    @JSBody(params = {}, script =
        "window.webmcFrameCount = 0;" +
        "window.webmcLastReport = 0;" +
        "window.webmcFPS = 0;" +
        "window.webmcAvgFrameTime = 0.0;" +
        "console.log('[WebMain] WebMC Performance Monitor Ready');"
    )
    private static native void initPerformanceMonitor();

    public static void main(String[] args) {
        initPerformanceMonitor();
        WebLog.info("========================================");
        WebLog.info("  WebMC 1.21.8 - High Performance Renderer");
        WebLog.info("========================================");
        LOGGER.info("WebMC starting...");

        // Initialize canvas and WebGL
        initOnCanvasReady();
    }

    private static void initOnCanvasReady() {
        // Get canvas
        canvas = getCanvas();
        if (canvas == null) {
            WebLog.error("Canvas not found! Make sure there's a <canvas id='game-canvas'> element.");
            LOGGER.error("Canvas not found! Make sure there's a <canvas id='game-canvas'> element.");
            showError("Canvas not found");
            return;
        }

        // Set canvas size
        canvas.setWidth(1280);
        canvas.setHeight(720);

        WebLog.info("Canvas found: " + canvas.getWidth() + "x" + canvas.getHeight());

        // Initialize WebGL
        initWebGL();
    }

    private static void initWebGL() {
        // Detect WebGL version
        WebGLVersionDetector.WebGLVersion version = WebGLVersionDetector.detect();

        WebLog.info("========================================");
        WebLog.info("WebGL Detection:");
        WebLog.info("  Version: " + version.name());
        WebLog.info("========================================");

        if (version == WebGLVersionDetector.WebGLVersion.NONE) {
            WebLog.error("WebGL not supported. Please use a modern browser.");
            showError("WebGL not supported. Please use a modern browser.");
            return;
        }

        // Get WebGL context
        WebGLRenderingContext gl = null;

        if (version == WebGLVersionDetector.WebGLVersion.WEBGL2) {
            gl = (WebGLRenderingContext) canvas.getContext("webgl2");
            if (gl != null) {
                WebLog.info("Using WebGL2 context (full feature set)");
            }
        }

        if (gl == null) {
            gl = (WebGLRenderingContext) canvas.getContext("webgl");
            if (gl != null) {
                WebLog.info("Using WebGL1 context (limited features)");
            }
        }

        if (gl == null) {
            gl = (WebGLRenderingContext) canvas.getContext("experimental-webgl");
            if (gl != null) {
                WebLog.info("Using experimental-webgl context");
            }
        }

        if (gl == null) {
            WebLog.error("Failed to create WebGL context");
            showError("Failed to create WebGL context");
            return;
        }

        // Install WebGL context
        WebGLContextHolder.install(gl);

        // Initialize GPU detection
        GpuDetector.GpuProfile profile = GpuDetector.detectProfile();

        WebLog.info("========================================");
        WebLog.info("GPU Information:");
        WebLog.info("  Vendor: " + profile.vendor);
        WebLog.info("  Renderer: " + profile.renderer);
        WebLog.info("  Performance Tier: " + profile.getTierName());
        WebLog.info("  Max Texture Size: " + profile.maxTextureSize);
        WebLog.info("  Instancing: " + (profile.supportsInstancing ? "Supported" : "Not supported"));
        WebLog.info("  Float Textures: " + (profile.supportsFloatTextures ? "Supported" : "Not supported"));
        WebLog.info("========================================");

        // Initialize rendering components
        initRendering();

        initialized = true;
        WebLog.info("========================================");
        WebLog.info("WebMC Initialized Successfully!");
        WebLog.info("========================================");

        // Start render loop
        startRenderLoop();
    }

    private static void initRendering() {
        WebGLRenderingContext gl = WebGLContextHolder.gl();

        // Initialize state cache
        WebGLStateCache.init(gl);

        // Initialize shader manager
        ShaderManager.init(gl);

        // Pre-compile shaders
        WebLog.info("Compiling shaders...");
        ShaderManager.useTerrainShader();
        ShaderManager.useEntityShader();
        ShaderManager.useParticlesShader();
        ShaderManager.useSkyShader();
        WebLog.info("All shaders compiled successfully");

        // Initialize render engine
        renderEngine = RenderEngine.getInstance();
        renderEngine.init();
        renderEngine.resize(canvas.getWidth(), canvas.getHeight());

        // Initialize texture manager
        TextureManager textureManager = TextureManager.getInstance();
        textureManager.init();

        // Initialize batch buffer
        BatchBuffer batchBuffer = renderEngine.getBatchBuffer();
        batchBuffer.init();

        // Apply adaptive render distance based on GPU
        GpuDetector.GpuProfile gpuProfile = GpuDetector.getProfile();
        AdaptiveRenderDistance adaptive = new AdaptiveRenderDistance(
            renderEngine, gpuProfile.tier);
        adaptive.apply();

        WebLog.info("Rendering components initialized");
        LOGGER.info("Rendering components initialized");
    }

    @JSBody(params = {"info"}, script = "console.log('[WebGL]', info);")
    private static native void logWebGLInfo(String info);

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (c && c.tagName === 'CANVAS') { return c; }" +
        "return null;"
    )
    private static native HTMLCanvasElement getCanvas();

    private static void showError(String message) {
        WebLog.error("WebMC Error: " + message);
        LOGGER.error("WebMC Error: {}", message);
        logError(message);
    }

    @JSBody(params = {"msg"}, script = "console.error('[WebMC]', msg);")
    private static native void logError(String msg);

    private static void startRenderLoop() {
        WebLog.info("Starting render loop...");

        // Initialize performance tracking
        fpsUpdateTime = getCurrentTimeMs();
        lastFrameTime = getCurrentTimeMs();

        // Start the render loop
        setupRenderLoop();
    }

    @JSBody(params = {}, script =
        "var _lastTime = performance.now();" +
        "var _frameCount = 0;" +
        "var _fpsUpdateTime = 0;" +
        "var _fps = 0;" +
        "var _frameTimeSum = 0.0;" +
        "var _frameTimeCount = 0;" +
        "var _minFrameTime = 999999.0;" +
        "var _maxFrameTime = 0.0;" +
        "window.webmcFrameCount = 0;" +
        "window.webmcLastReport = performance.now();" +
        "window.webmcFPS = 0;" +
        "window.webmcAvgFrameTime = 0.0;" +
        "window.webmcMinFrameTime = 0.0;" +
        "window.webmcMaxFrameTime = 0.0;" +
        "" +
        "function _renderLoop(currentTime) {" +
        "  var _deltaTime = currentTime - _lastTime;" +
        "  _lastTime = currentTime;" +
        "  _frameCount = _frameCount + 1;" +
        "  _frameTimeSum = _frameTimeSum + _deltaTime;" +
        "  _frameTimeCount = _frameTimeCount + 1;" +
        "  if (_deltaTime < _minFrameTime) { _minFrameTime = _deltaTime; }" +
        "  if (_deltaTime > _maxFrameTime) { _maxFrameTime = _deltaTime; }" +
        "" +
        "  if (currentTime - _fpsUpdateTime >= 1000) {" +
        "    _fps = Math.round((_frameCount * 1000) / (currentTime - _fpsUpdateTime));" +
        "    var _avgFrameTime = _frameTimeSum / _frameTimeCount;" +
        "    _fpsUpdateTime = currentTime;" +
        "    _frameCount = 0;" +
        "    _frameTimeSum = 0.0;" +
        "    _frameTimeCount = 0;" +
        "    window.webmcFPS = _fps;" +
        "    window.webmcAvgFrameTime = _avgFrameTime;" +
        "    window.webmcMinFrameTime = _minFrameTime;" +
        "    window.webmcMaxFrameTime = _maxFrameTime;" +
        "    _minFrameTime = 999999.0;" +
        "    _maxFrameTime = 0.0;" +
        "  }" +
        "" +
        "  window.webmcFrameCount = window.webmcFrameCount + 1;" +
        "  window.requestAnimationFrame(_renderLoop);" +
        "}" +
        "" +
        "window.requestAnimationFrame(_renderLoop);" +
        "console.log('[WebMain] High-Performance Render Loop Started');" +
        "console.log('       Performance monitoring: FPS, Frame Time, Min/Max');"
    )
    private static native void setupRenderLoop();

    @JSBody(params = {}, script = "return performance.now();")
    private static native long getCurrentTimeMs();

    /**
     * Get current FPS from browser performance monitor.
     */
    @JSBody(params = {}, script = "return window.webmcFPS || 0;")
    public static native int getFPS();

    /**
     * Get average frame time from browser.
     */
    @JSBody(params = {}, script = "return window.webmcAvgFrameTime || 0.0;")
    public static native float getAvgFrameTime();

    /**
     * Get minimum frame time.
     */
    @JSBody(params = {}, script = "return window.webmcMinFrameTime || 0.0;")
    public static native float getMinFrameTime();

    /**
     * Get maximum frame time.
     */
    @JSBody(params = {}, script = "return window.webmcMaxFrameTime || 0.0;")
    public static native float getMaxFrameTime();

    /**
     * Get total frames rendered.
     */
    @JSBody(params = {}, script = "return window.webmcFrameCount || 0;")
    public static native int getFrameCount();

    /**
     * Get detailed performance report.
     */
    public static String getPerformanceReport() {
        int fps = getFPS();
        float avgFrame = getAvgFrameTime();
        float minFrame = getMinFrameTime();
        float maxFrame = getMaxFrameTime();
        int frames = getFrameCount();

        RenderStats stats = null;
        if (renderEngine != null && renderEngine.getBatchBuffer() != null) {
            stats = renderEngine.getBatchBuffer().getStats();
        }

        StringBuilder report = new StringBuilder();
        report.append("\n========================================\n");
        report.append("   WebMC Performance Report\n");
        report.append("========================================\n");
        report.append(String.format("  FPS: %d\n", fps));
        report.append(String.format("  Frame Time: %.2fms (avg)\n", avgFrame));
        report.append(String.format("  Frame Time Range: %.2fms - %.2fms\n", minFrame, maxFrame));
        report.append(String.format("  Total Frames: %d\n", frames));

        if (stats != null) {
            report.append("  ----------------------------------------\n");
            report.append("  Rendering Statistics:\n");
            report.append(String.format("    GPU Tier: %s\n", stats.gpuTier));
            report.append(String.format("    Batches: %d\n", stats.batchesSubmitted));
            report.append(String.format("    Draw Calls: %d\n", stats.drawCalls));
            report.append(String.format("    Vertices: %s\n", formatCount(stats.verticesSubmitted)));
            report.append(String.format("    Triangles: %s\n", formatCount(stats.trianglesSubmitted)));
        }

        report.append("========================================\n");

        return report.toString();
    }

    private static String formatCount(long count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    /**
     * Log performance report to console.
     */
    @JSBody(params = {}, script =
        "var report = window.webmcGetPerformanceReport ? window.webmcGetPerformanceReport() : 'Report not available';" +
        "console.log(report);"
    )
    public static native void logPerformanceReport();

    /**
     * Check if WebMC is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get render engine instance.
     */
    public static RenderEngine getRenderEngine() {
        return renderEngine;
    }
}
