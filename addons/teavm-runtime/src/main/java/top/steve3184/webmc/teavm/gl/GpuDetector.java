package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.webgl.WebGLRenderingContext;
import top.steve3184.webmc.teavm.WebLog;

/**
 * GPU detection and performance tier classification.
 * Detects GPU capabilities and assigns a performance tier for optimizations.
 */
public final class GpuDetector {

    public enum Tier {
        ULTRA(4, "Ultra"),
        HIGH(3, "High"),
        MEDIUM(2, "Medium"),
        LOW(1, "Low"),
        FALLBACK(0, "Fallback");

        public final int level;
        public final String name;

        Tier(int level, String name) {
            this.level = level;
            this.name = name;
        }
    }

    public static class GpuProfile {
        public final Tier tier;
        public final String vendor;
        public final String renderer;
        public final int maxTextureSize;
        public final int maxVertexAttribs;
        public final boolean supportsFloatTextures;
        public final boolean supportsInstancing;

        public GpuProfile(Tier tier, String vendor, String renderer,
                         int maxTextureSize, int maxVertexAttribs,
                         boolean supportsFloatTextures, boolean supportsInstancing) {
            this.tier = tier;
            this.vendor = vendor;
            this.renderer = renderer;
            this.maxTextureSize = maxTextureSize;
            this.maxVertexAttribs = maxVertexAttribs;
            this.supportsFloatTextures = supportsFloatTextures;
            this.supportsInstancing = supportsInstancing;
        }

        public String getTierName() {
            return tier.name;
        }

        public int getTierLevel() {
            return tier.level;
        }
    }

    private static GpuProfile cachedProfile = null;

    private GpuDetector() {}

    /**
     * Detect and return GPU profile with performance tier.
     */
    public static GpuProfile detectProfile() {
        if (cachedProfile != null) {
            return cachedProfile;
        }

        WebGLRenderingContext gl = WebGLContextHolder.gl();
        if (gl == null) {
            cachedProfile = new GpuProfile(Tier.FALLBACK, "Unknown", "No WebGL",
                2048, 8, false, false);
            return cachedProfile;
        }

        String vendor = getString(gl, WebGLRenderingContext.VENDOR);
        String renderer = getString(gl, WebGLRenderingContext.RENDERER);
        int maxTextureSize = getInt(gl, WebGLRenderingContext.MAX_TEXTURE_SIZE);
        int maxVertexAttribs = getInt(gl, WebGLRenderingContext.MAX_VERTEX_ATTRIBS);

        // Check for float texture support
        boolean supportsFloatTextures = checkFloatTextureSupport(gl);

        // WebGL2 has instancing, WebGL1 doesn't natively
        boolean supportsInstancing = supportsWebGL2();

        // Calculate performance score
        int score = calculateScore(renderer, maxTextureSize, maxVertexAttribs,
            supportsFloatTextures, supportsInstancing);

        Tier tier = getTierFromScore(score, renderer);

        cachedProfile = new GpuProfile(tier, vendor, renderer, maxTextureSize,
            maxVertexAttribs, supportsFloatTextures, supportsInstancing);

        log("[GpuDetector] Detected: " + vendor + " / " + renderer);
        log("[GpuDetector] Tier: " + tier.name + " (score: " + score + ")");

        return cachedProfile;
    }

    /**
     * Check if WebGL2 is available.
     */
    private static boolean supportsWebGL2() {
        return WebGLVersionDetector.detect() == WebGLVersionDetector.WebGLVersion.WEBGL2;
    }

    /**
     * Get the cached GPU profile.
     */
    public static GpuProfile getProfile() {
        if (cachedProfile == null) {
            detectProfile();
        }
        return cachedProfile;
    }

    /**
     * Calculate performance score based on GPU capabilities.
     */
    private static int calculateScore(String renderer, int maxTextureSize,
            int maxVertexAttribs, boolean supportsFloatTextures, boolean supportsInstancing) {
        int score = 0;

        if (renderer == null) return 0;

        // Renderer-based scoring
        String r = renderer.toLowerCase();

        // High-end GPUs
        if (r.contains("nvidia") || r.contains("geforce rtx") ||
            r.contains("radeon rx") || r.contains("apple m")) {
            score += 5;
        } else if (r.contains("radeon") || r.contains("geforce gtx") ||
                   r.contains("intel iris") || r.contains("apple")) {
            score += 3;
        } else if (r.contains("intel") || r.contains("adreno")) {
            score += 2;
        } else if (r.contains("mali") || r.contains("powervr")) {
            score += 1;
        }

        // Texture size scoring
        if (maxTextureSize >= 16384) score += 2;
        else if (maxTextureSize >= 8192) score += 1;

        // Feature scoring
        if (supportsFloatTextures) score += 1;
        if (supportsInstancing) score += 1;

        return score;
    }

    /**
     * Determine tier from score.
     */
    private static Tier getTierFromScore(int score, String renderer) {
        if (score >= 8) return Tier.ULTRA;
        if (score >= 6) return Tier.HIGH;
        if (score >= 4) return Tier.MEDIUM;
        if (score >= 2) return Tier.LOW;
        return Tier.FALLBACK;
    }

    /**
     * Check if float textures are supported.
     */
    private static boolean checkFloatTextureSupport(WebGLRenderingContext gl) {
        return checkExtension(gl, "EXT_color_buffer_float") ||
               checkExtension(gl, "OES_texture_float");
    }

    @JSBody(params = {"gl", "ext"}, script =
        "return gl.getExtension(ext) !== null;"
    )
    private static native boolean checkExtension(WebGLRenderingContext gl, String ext);

    @JSBody(params = {"gl", "pname"}, script =
        "try { var v = gl.getParameter(pname); return (v === undefined || v === null) ? '' : String(v); } catch(e) { return ''; }"
    )
    private static native String getString(WebGLRenderingContext gl, int pname);

    @JSBody(params = {"gl", "pname"}, script =
        "try { var v = gl.getParameter(pname); return (v === undefined || v === null) ? 0 : (v | 0); } catch(e) { return 0; }"
    )
    private static native int getInt(WebGLRenderingContext gl, int pname);

    private static void log(String msg) {
        WebLog.info(msg);
    }
}
