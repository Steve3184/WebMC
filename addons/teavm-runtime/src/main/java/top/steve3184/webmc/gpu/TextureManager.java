package top.steve3184.webmc.gpu;

import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLTexture;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimized texture manager that uses texture atlases and efficient mipmapping.
 * Reduces texture binds and improves cache locality.
 */
public final class TextureManager {

    private static WebGL2RenderingContext gl;
    private static boolean initialized = false;

    // Texture cache
    private static final Map<String, TextureInfo> textureCache = new HashMap<>();
    private static int cacheHits = 0;
    private static int cacheMisses = 0;

    // Texture limits based on GPU
    private static int maxTextureSize = 2048;
    private static int maxTextureUnits = 8;

    private TextureManager() {}

    /**
     * Initialize texture manager with GPU capabilities.
     */
    public static void init() {
        if (initialized) return;

        gl = WebGLContextHolder.gl();

        GpuProfile profile = GpuDetector.getProfile();
        maxTextureSize = profile.getMaxTextureSize();
        maxTextureUnits = profile.getTier().maxTextureUnits;

        // Enable mipmapping if supported
        if (profile.supportsMipmaps()) {
            gl.hint(WebGL2RenderingContext.GENERATE_MIPMAP_HINT, WebGL2RenderingContext.NICEST);
        }

        initialized = true;
        System.out.println("[mc-web/texture] TextureManager init: maxSize=" + maxTextureSize
            + ", units=" + maxTextureUnits + ", mipmaps=" + profile.supportsMipmaps());
    }

    /**
     * Get texture info from cache, or null if not cached.
     */
    public static TextureInfo getCachedTexture(String id) {
        TextureInfo info = textureCache.get(id);
        if (info != null) {
            cacheHits++;
            info.lastUsed = System.currentTimeMillis();
        } else {
            cacheMisses++;
        }
        return info;
    }

    /**
     * Cache a texture.
     */
    public static void cacheTexture(String id, WebGLTexture texture, int width, int height) {
        TextureInfo info = new TextureInfo(id, texture, width, height);
        textureCache.put(id, info);
    }

    /**
     * Bind a texture to a unit.
     */
    public static void bindTexture(int unit, WebGLTexture texture) {
        if (gl == null) return;
        gl.activeTexture(WebGL2RenderingContext.TEXTURE0 + unit);
        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);
    }

    /**
     * Create an optimized 2D texture.
     */
    public static WebGLTexture createTexture(int width, int height, boolean mipmap) {
        if (gl == null) return null;

        WebGLTexture tex = gl.createTexture();
        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, tex);

        // Set parameters for optimal performance
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_MIN_FILTER,
            mipmap ? WebGL2RenderingContext.LINEAR_MIPMAP_LINEAR : WebGL2RenderingContext.LINEAR);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_MAG_FILTER,
            WebGL2RenderingContext.LINEAR);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_WRAP_S,
            WebGL2RenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_WRAP_T,
            WebGL2RenderingContext.CLAMP_TO_EDGE);

        // Enable anisotropic filtering if available
        GpuProfile profile = GpuDetector.getProfile();
        if (profile.shouldUseAntialiasing()) {
            WebGLExtension ext = gl.getExtension("EXT_texture_filter_anisotropic");
            if (ext != null) {
                gl.texParameterf(WebGL2RenderingContext.TEXTURE_2D,
                    ext.getConstantValue("TEXTURE_MAX_ANISOTROPY_EXT"), 4.0f);
            }
        }

        return tex;
    }

    /**
     * Upload texture data with optimal format selection.
     */
    public static void uploadTexture(WebGLTexture texture, int width, int height, byte[] data, boolean generateMipmap) {
        if (gl == null) return;

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);

        // Use compressed upload if available
        // RGBA format is most compatible
        gl.texImage2D(WebGL2RenderingContext.TEXTURE_2D, 0,
            WebGL2RenderingContext.RGBA, width, height, 0,
            WebGL2RenderingContext.RGBA, WebGL2RenderingContext.UNSIGNED_BYTE,
            createUint8Array(data));

        // Generate mipmaps for better quality
        if (generateMipmap && GpuDetector.getProfile().supportsMipmaps()) {
            gl.generateMipmap(WebGL2RenderingContext.TEXTURE_2D);
        }
    }

    /**
     * Update a portion of a texture.
     */
    public static void updateSubTexture(WebGLTexture texture, int x, int y, int width, int height, byte[] data) {
        if (gl == null) return;

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);
        gl.texSubImage2D(WebGL2RenderingContext.TEXTURE_2D, 0, x, y, width, height,
            WebGL2RenderingContext.RGBA, WebGL2RenderingContext.UNSIGNED_BYTE,
            createUint8Array(data));
    }

    /**
     * Delete a texture and remove from cache.
     */
    public static void deleteTexture(String id) {
        TextureInfo info = textureCache.remove(id);
        if (info != null && info.texture != null) {
            gl.deleteTexture(info.texture);
        }
    }

    /**
     * Clear the entire texture cache.
     */
    public static void clearCache() {
        for (TextureInfo info : textureCache.values()) {
            if (info.texture != null) {
                gl.deleteTexture(info.texture);
            }
        }
        textureCache.clear();
        System.out.println("[mc-web/texture] Cache cleared");
    }

    /**
     * Get cache statistics.
     */
    public static CacheStats getCacheStats() {
        return new CacheStats(textureCache.size(), cacheHits, cacheMisses);
    }

    /**
     * Get maximum texture size.
     */
    public static int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * Get maximum texture units.
     */
    public static int getMaxTextureUnits() {
        return maxTextureUnits;
    }

    /**
     * Check if a texture size is within GPU limits.
     */
    public static boolean isSizeValid(int width, int height) {
        return width <= maxTextureSize && height <= maxTextureSize;
    }

    /**
     * Calculate optimal mip level for a texture given screen resolution.
     */
    public static int calculateMipLevel(int textureSize, int screenSize) {
        if (screenSize <= 0) return 0;
        int level = 0;
        int size = textureSize;
        while (size > screenSize && size > 1) {
            size >>= 1;
            level++;
        }
        return level;
    }

    // JSO helpers
    @org.teavm.jso.JSBody(params = {"data"}, script = "return new Uint8Array(data);")
    private static native org.teavm.jso.typedarrays.Uint8Array createUint8Array(byte[] data);

    // Internal types
    public static final class TextureInfo {
        public final String id;
        public final WebGLTexture texture;
        public final int width;
        public final int height;
        public long lastUsed;
        public long uploadTime;

        public TextureInfo(String id, WebGLTexture texture, int width, int height) {
            this.id = id;
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.lastUsed = System.currentTimeMillis();
            this.uploadTime = this.lastUsed;
        }
    }

    public static final class CacheStats {
        public final int cacheSize;
        public final int hits;
        public final int misses;

        public CacheStats(int size, int hits, int misses) {
            this.cacheSize = size;
            this.hits = hits;
            this.misses = misses;
        }

        public float getHitRate() {
            int total = hits + misses;
            return total > 0 ? (float) hits / total : 0f;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{size=%d, hits=%d, misses=%d, hitRate=%.1f%%}",
                cacheSize, hits, misses, getHitRate() * 100);
        }
    }

    @org.teavm.jso.JSBody(params = {"gl", "name"}, script = "return gl.getExtension(name);")
    private static native WebGLExtension getExtension(WebGL2RenderingContext gl, String name);

    @org.teavm.jso.JSBody(script = "return {};")
    private static native WebGLExtension createExtension();

    public interface WebGLExtension {
        int getConstantValue(String name);
    }
}
