package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLContextAttributes;
import org.teavm.jso.webgl.WebGLExtension;
import top.steve3184.webmc.gpu.GpuProfile;

/**
 * Detects GPU capabilities and creates optimized rendering profile.
 * Runs once at startup to determine hardware tier and configure rendering.
 */
public final class GpuDetector {

    private static GpuProfile cachedProfile;

    /**
     * Detect GPU and create optimized profile.
     * Results are cached for subsequent calls.
     */
    public static GpuProfile detectGpu() {
        if (cachedProfile != null) {
            return cachedProfile;
        }

        GpuProfile.Builder builder = new GpuProfile.Builder();

        // Try to get WebGL2 context
        WebGL2RenderingContext gl = tryCreateWebGL2Context();
        boolean hasWebGL2 = gl != null;

        if (hasWebGL2) {
            detectWebGL2Capabilities(builder, gl);
        } else {
            // Fallback to WebGL1
            builder.setHasWebGL2(false)
                   .setTier(GpuProfile.Tier.LOW);
            cachedProfile = builder.build();
            return cachedProfile;
        }

        cachedProfile = builder.build();
        return cachedProfile;
    }

    private static WebGL2RenderingContext tryCreateWebGL2Context() {
        try {
            return getCanvasContext("webgl2");
        } catch (Exception e) {
            // Try experimental-webgl
            try {
                return getCanvasContext("experimental-webgl");
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @JSBody(script =
        "var canvas = document.getElementById('canvas');" +
        "if (!canvas) canvas = document.createElement('canvas');" +
        "return canvas.getContext(ctx, {antialias: false, alpha: false, depth: false, stencil: false, preserveDrawingBuffer: false, powerPreference: 'high-performance'});"
    )
    private static native WebGL2RenderingContext getCanvasContext(String ctx);

    private static void detectWebGL2Capabilities(GpuProfile.Builder builder, WebGL2RenderingContext gl) {
        // Basic limits
        int maxTextureSize = gl.getParameteri(WebGL2RenderingContext.MAX_TEXTURE_SIZE);
        int maxVertexAttribs = gl.getParameteri(WebGL2RenderingContext.MAX_VERTEX_ATTRIBS);

        builder.setMaxTextureSize(maxTextureSize)
               .setMaxVertexAttribs(maxVertexAttribs);

        // Check extensions
        boolean hasDepthTexture = gl.getExtension("WEBGL_depth_texture") != null ||
                                  gl.getExtension("WEBGL2_depth_texture") != null;
        boolean hasFloatTextures = gl.getExtension("OES_texture_float") != null ||
                                   gl.getExtension("EXT_color_buffer_float") != null;
        boolean hasInstancing = true; // WebGL2 always has instancing

        builder.setSupportsDepthTexture(hasDepthTexture)
               .setSupportsFloatTextures(hasFloatTextures)
               .setSupportsInstancing(hasInstancing)
               .setHasWebGL2(true);

        // Get renderer info for tier inference
        WebGLExtension debugInfo = gl.getExtension("WEBGL_debug_renderer_info");
        String renderer = null;
        if (debugInfo != null) {
            renderer = gl.getParameterString(debugInfo.getConstantValue("UNMASKED_RENDERER_WEBGL"));
        }

        // If we can't get renderer, use capability-based inference
        if (renderer == null || renderer.isEmpty()) {
            inferTierFromCapabilities(builder, maxTextureSize, maxVertexAttribs, hasFloatTextures);
        } else {
            builder.inferTierFromRenderer(renderer);
        }

        // Apply power preference hints
        applyPowerPreferenceHints(builder);
    }

    private static void inferTierFromCapabilities(GpuProfile.Builder builder, int maxTextureSize,
                                                   int maxVertexAttribs, boolean hasFloatTextures) {
        int score = 0;

        // Texture size scoring
        if (maxTextureSize >= 16384) score += 3;
        else if (maxTextureSize >= 8192) score += 2;
        else if (maxTextureSize >= 4096) score += 1;

        // Vertex attribs scoring
        if (maxVertexAttribs >= 16) score += 2;
        else if (maxVertexAttribs >= 8) score += 1;

        // Float textures
        if (hasFloatTextures) score += 2;

        // Map score to tier
        GpuProfile.Tier tier;
        if (score >= 6) tier = GpuProfile.Tier.HIGH;
        else if (score >= 4) tier = GpuProfile.Tier.MEDIUM;
        else tier = GpuProfile.Tier.LOW;

        builder.setTier(tier);
    }

    private static void applyPowerPreferenceHints(GpuProfile.Builder builder) {
        // On mobile/low-power devices, reduce default render distance
        if (isMobileDevice()) {
            if (builder.tier == GpuProfile.Tier.HIGH) {
                builder.setTier(GpuProfile.Tier.MEDIUM);
            }
            if (builder.renderDistance == GpuProfile.RenderDistance.FAR ||
                builder.renderDistance == GpuProfile.RenderDistance.EXTREME) {
                builder.setRenderDistance(GpuProfile.RenderDistance.NORMAL);
            }
        }
    }

    @JSBody(script =
        "return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);"
    )
    private static native boolean isMobileDevice();

    /**
     * Run a quick benchmark to refine tier detection.
     * This is called asynchronously after initial render.
     */
    public static void runQuickBenchmark(WebGL2RenderingContext gl, BenchmarkCallback callback) {
        // Simple benchmark: measure texture upload speed
        int iterations = 100;
        int textureSize = 256;

        // Create test texture
        org.teavm.jso.webgl.WebGLTexture testTex = gl.createTexture();
        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, testTex);

        // Allocate test data
        byte[] data = new byte[textureSize * textureSize * 4];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }

        // Benchmark texture uploads
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            gl.texImage2D(WebGL2RenderingContext.TEXTURE_2D, 0,
                         WebGL2RenderingContext.RGBA, textureSize, textureSize, 0,
                         WebGL2RenderingContext.RGBA, WebGL2RenderingContext.UNSIGNED_BYTE,
                         createUint8Array(data));
        }
        long endTime = System.currentTimeMillis();

        // Cleanup
        gl.deleteTexture(testTex);

        // Calculate operations per second
        long duration = endTime - startTime;
        double opsPerSec = (iterations * 1000.0) / duration;

        // Refine tier based on benchmark results
        GpuProfile.Tier newTier;
        if (opsPerSec > 500) newTier = GpuProfile.Tier.ULTRA;
        else if (opsPerSec > 200) newTier = GpuProfile.Tier.HIGH;
        else if (opsPerSec > 50) newTier = GpuProfile.Tier.MEDIUM;
        else newTier = GpuProfile.Tier.LOW;

        if (cachedProfile != null && newTier.level < cachedProfile.getTier().level) {
            // Downgrade tier if benchmark shows poor performance
            cachedProfile = new GpuProfile.Builder()
                .setTier(newTier)
                .setMaxTextureSize(cachedProfile.getMaxTextureSize())
                .setHasWebGL2(cachedProfile.hasWebGL2())
                .build();
        }

        callback.onComplete(newTier, opsPerSec);
    }

    @JSBody(params = {"data"}, script =
        "return new Uint8Array(data);"
    )
    private static native org.teavm.jso.typedarrays.Int8Array createUint8Array(byte[] data);

    public interface BenchmarkCallback {
        void onComplete(GpuProfile.Tier detectedTier, double opsPerSecond);
    }

    /**
     * Get cached profile, or detect if not yet done.
     */
    public static GpuProfile getProfile() {
        if (cachedProfile == null) {
            return detectGpu();
        }
        return cachedProfile;
    }

    /**
     * Reset cached profile to force re-detection.
     * Useful for testing or dynamic quality changes.
     */
    public static void reset() {
        cachedProfile = null;
    }
}
