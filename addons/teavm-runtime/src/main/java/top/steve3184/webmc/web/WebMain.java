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
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.ShaderManager;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;
import top.steve3184.webmc.teavm.gl.WebGLStateCache;
import top.steve3184.webmc.teavm.gl.WebGLVersionDetector;
import top.steve3184.webmc.teavm.gl.render.AdaptiveRenderDistance;
import top.steve3184.webmc.teavm.gl.render.BatchBuffer;
import top.steve3184.webmc.teavm.gl.render.RenderEngine;
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

    @JSBody(params = {}, script =
        "window.webmcStartTime = window.webmcStartTime || performance.now();" +
        "console.log('[WebMain] Page loaded, waiting for user click to start...');"
    )
    private static native void logStart();

    public static void main(String[] args) {
        logStart();
        LOGGER.info("WebMC starting...");

        // Wait for canvas initialization
        initOnCanvasReady();
    }

    private static void initOnCanvasReady() {
        // Get canvas
        canvas = getCanvas();
        if (canvas == null) {
            LOGGER.error("Canvas not found! Make sure there's a <canvas id='game-canvas'> element.");
            showError("Canvas not found");
            return;
        }

        // Set canvas size
        canvas.setWidth(854);
        canvas.setHeight(480);

        // Initialize WebGL
        initWebGL();
    }

    private static void initWebGL() {
        // Detect WebGL version
        WebGLVersionDetector.WebGLVersion version = WebGLVersionDetector.detect();

        LOGGER.info("WebGL version detected: {}", version.name());
        logWebGLInfo(version.name());

        if (version == WebGLVersionDetector.WebGLVersion.NONE) {
            showError("WebGL not supported. Please use a modern browser.");
            return;
        }

        // Get WebGL context
        WebGLRenderingContext gl = null;

        if (version == WebGLVersionDetector.WebGLVersion.WEBGL2) {
            gl = (WebGLRenderingContext) canvas.getContext("webgl2");
        }

        if (gl == null) {
            gl = (WebGLRenderingContext) canvas.getContext("webgl");
        }

        if (gl == null) {
            gl = (WebGLRenderingContext) canvas.getContext("experimental-webgl");
        }

        if (gl == null) {
            showError("Failed to create WebGL context");
            return;
        }

        // Install WebGL context
        WebGLContextHolder.install(gl);

        // Initialize GPU detection
        GpuDetector.GpuProfile profile = GpuDetector.detectProfile();
        LOGGER.info("GPU: {} / {} (Tier: {})",
            profile.vendor, profile.renderer, profile.getTierName());

        // Initialize rendering components
        initRendering();

        initialized = true;
        LOGGER.info("WebMC initialized successfully");

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
        ShaderManager.getProgram("basic3d");
        ShaderManager.getProgram("basic2d");
        ShaderManager.getProgram("sky");

        // Initialize render engine
        renderEngine = RenderEngine.getInstance();
        renderEngine.init();
        renderEngine.resize(canvas.getWidth(), canvas.getHeight());

        // Initialize texture manager
        TextureManager textureManager = TextureManager.getInstance();

        // Initialize batch buffer
        BatchBuffer batchBuffer = renderEngine.getBatchBuffer();
        batchBuffer.init();

        LOGGER.info("Rendering components initialized");
    }

    @JSBody(params = {"info"}, script = "console.log('[WebGL Info]', info);")
    private static native void logWebGLInfo(String info);

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (c && c.tagName === 'CANVAS') { return c; }" +
        "return null;"
    )
    private static native HTMLCanvasElement getCanvas();

    @JSBody(params = {"width", "height"}, script =
        "document.body.style.margin = '0';" +
        "document.body.style.overflow = 'hidden';" +
        "document.body.style.backgroundColor = '#1a1a2e';" +
        "var canvas = document.getElementById('game-canvas');" +
        "if (!canvas) {" +
        "  canvas = document.createElement('canvas');" +
        "  canvas.id = 'game-canvas';" +
        "  document.body.appendChild(canvas);" +
        "}" +
        "canvas.width = width;" +
        "canvas.height = height;" +
        "canvas.style.display = 'block';" +
        "canvas.style.margin = 'auto';" +
        "canvas.style.position = 'absolute';" +
        "canvas.style.top = '0';" +
        "canvas.style.left = '0';" +
        "canvas.style.right = '0';" +
        "canvas.style.bottom = '0';" +
        "return canvas;"
    )
    private static native HTMLCanvasElement createCanvas(int width, int height);

    private static void showError(String message) {
        LOGGER.error("WebMC Error: {}", message);
        logError(message);
    }

    @JSBody(params = {"msg"}, script = "console.error('[WebMC]', msg);")
    private static native void logError(String msg);

    private static void startRenderLoop() {
        // Set up render loop using requestAnimationFrame
        setupRenderLoop();
    }

    @JSBody(params = {}, script =
        "var loop = function() {" +
        "  try {" +
        "    // Update WebGL FPS counter" +
        "    var gl = window.__webmcGl;" +
        "    if (gl) {" +
        "      window.__webmcFps = window.__webmcFps || 0;" +
        "      window.__webmcFrames = window.__webmcFrames || 0;" +
        "      window.__webmcLastTime = window.__webmcLastTime || performance.now();" +
        "      window.__webmcFrames++;" +
        "      var now = performance.now();" +
        "      if (now - window.__webmcLastTime >= 1000) {" +
        "        window.__webmcFps = window.__webmcFrames * 1000 / (now - window.__webmcLastTime);" +
        "        window.__webmcFrames = 0;" +
        "        window.__webmcLastTime = now;" +
        "      }" +
        "    }" +
        "  } catch(e) { console.error('Render loop error:', e); }" +
        "  window.requestAnimationFrame(loop);" +
        "};" +
        "window.requestAnimationFrame(loop);" +
        "console.log('[WebMain] Render loop started');"
    )
    private static native void setupRenderLoop();
}
