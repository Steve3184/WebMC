package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLContextAttributes;
import org.teavm.jso.webgl.WebGLTexture;
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

        // Try to get WebGL2 context
        WebGL2RenderingContext gl = tryCreateWebGL2Context();
        boolean hasWebGL2 = gl != null;

        if (!hasWebGL2) {
            // Fallback to basic profile
            cachedProfile = GpuProfile.builder()
                .hasWebGL2(false)
                .tier(GpuProfile.Tier.LOW)
                .build();
            return cachedProfile;
        }

        // Detect capabilities
        int maxTextureSize = gl.getParameteri(WebGL2RenderingContext.MAX_TEXTURE_SIZE);
        int maxVertexAttribs = gl.getParameteri(WebGL2RenderingContext.MAX_VERTEX_ATTRIBS);

        // Check extensions
        JSObject depthExt = gl.getExtension("WEBGL_depth_texture");
        JSObject floatExt = gl.getExtension("OES_texture_float");
        boolean hasDepthTexture = depthExt != null;
        boolean hasFloatTextures = floatExt != null;

        // Get renderer info for tier inference
        String renderer = getUnmaskedRenderer(gl);

        // Determine tier
        GpuProfile.Tier tier = inferTier(renderer, maxTextureSize, maxVertexAttribs, hasFloatTextures);

        // On mobile, reduce tier
        if (isMobileDevice()) {
            if (tier == GpuProfile.Tier.ULTRA) tier = GpuProfile.Tier.HIGH;
            else if (tier == GpuProfile.Tier.HIGH) tier = GpuProfile.Tier.MEDIUM;
        }

        // Determine render distance based on tier
        GpuProfile.RenderDistance renderDist = GpuProfile.RenderDistance.NORMAL;
        if (tier == GpuProfile.Tier.HIGH || tier == GpuProfile.Tier.ULTRA) {
            renderDist = GpuProfile.RenderDistance.FAR;
        }

        cachedProfile = GpuProfile.builder()
            .hasWebGL2(true)
            .tier(tier)
            .renderDistance(renderDist)
            .maxTextureSize(maxTextureSize)
            .maxVertexAttribs(maxVertexAttribs)
            .supportsDepthTexture(hasDepthTexture)
            .supportsFloatTextures(hasFloatTextures)
            .supportsInstancing(true) // WebGL2 always has instancing
            .build();

        return cachedProfile;
    }

    private static WebGL2RenderingContext tryCreateWebGL2Context() {
        return getCanvasContextInternal();
    }

    @JSBody(script =
        "var canvas = document.getElementById('canvas');" +
        "if (!canvas) canvas = document.createElement('canvas');" +
        "canvas.width = 1; canvas.height = 1;" +
        "return canvas.getContext('webgl2', {antialias: false, alpha: false, depth: false, stencil: false, powerPreference: 'high-performance'});"
    )
    private static native WebGL2RenderingContext getCanvasContextInternal();

    /**
     * Get unmasked renderer string using raw JS.
     */
    private static String getUnmaskedRenderer(WebGL2RenderingContext gl) {
        try {
            JSObject debugInfo = gl.getExtension("WEBGL_debug_renderer_info");
            if (debugInfo != null) {
                return getRendererString(gl);
            }
        } catch (Exception e) {
            // Extension not available
        }
        return "";
    }

    @JSBody(params = {"gl"}, script =
        "var ext = gl.getExtension('WEBGL_debug_renderer_info');" +
        "if (ext) return gl.getParameter(ext.UNMASKED_RENDERER_WEBGL);" +
        "return '';"
    )
    private static native String getRendererString(WebGL2RenderingContext gl);

    private static GpuProfile.Tier inferTier(String renderer, int maxTextureSize,
                                             int maxVertexAttribs, boolean hasFloatTextures) {
        // High score = better GPU
        int score = 0;

        // Known high-end GPUs
        if (renderer != null && !renderer.isEmpty()) {
            String r = renderer.toLowerCase();
            if (r.contains("nvidia") || r.contains("geforce")) {
                score += 3;
                if (r.contains("rtx") || r.contains("gtx 10") || r.contains("gtx 16")) score += 2;
                if (r.contains("gtx 20") || r.contains("gtx 30") || r.contains("gtx 40")) score += 2;
            } else if (r.contains("amd") || r.contains("radeon")) {
                score += 2;
                if (r.contains("rx 5") || r.contains("rx 6") || r.contains("rx 7")) score += 2;
            } else if (r.contains("apple") || r.contains("m1") || r.contains("m2") || r.contains("m3")) {
                score += 3;
            } else if (r.contains("intel")) {
                score += 1;
                if (r.contains("iris") || r.contains("uhd 6")) score += 1;
            }
        }

        // Texture size scoring
        if (maxTextureSize >= 16384) score += 2;
        else if (maxTextureSize >= 8192) score += 1;

        // Vertex attribs scoring
        if (maxVertexAttribs >= 16) score += 1;

        // Float textures
        if (hasFloatTextures) score += 1;

        // Mobile gets one tier lower
        if (isMobileDevice()) score -= 2;

        // Map score to tier
        if (score >= 7) return GpuProfile.Tier.ULTRA;
        if (score >= 5) return GpuProfile.Tier.HIGH;
        if (score >= 3) return GpuProfile.Tier.MEDIUM;
        return GpuProfile.Tier.LOW;
    }

    @JSBody(script =
        "return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);"
    )
    private static native boolean isMobileDevice();

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
     */
    public static void reset() {
        cachedProfile = null;
    }
}
