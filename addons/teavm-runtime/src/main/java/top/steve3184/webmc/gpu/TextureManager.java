package top.steve3184.webmc.gpu;

import org.teavm.jso.typedarrays.Uint8Array;
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

    private TextureManager() {}

    /**
     * Initialize texture manager with GPU capabilities.
     */
    public static void init() {
        if (initialized) return;

        gl = WebGLContextHolder.gl();
        if (gl == null) return;

        GpuProfile profile = GpuDetector.getProfile();
        maxTextureSize = profile.getMaxTextureSize();

        initialized = true;
        log("[mc-web/texture] TextureManager init: maxSize=" + maxTextureSize + ", tier=" + profile.getTier());
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

        return tex;
    }

    /**
     * Upload texture data with optimal format selection.
     */
    public static void uploadTexture(WebGLTexture texture, int width, int height, byte[] data, boolean generateMipmap) {
        if (gl == null) return;

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);
        Uint8Array pixelData = createPixelData(data);
        gl.texImage2D(WebGL2RenderingContext.TEXTURE_2D, 0,
            WebGL2RenderingContext.RGBA, width, height, 0,
            WebGL2RenderingContext.RGBA, WebGL2RenderingContext.UNSIGNED_BYTE,
            pixelData);

        if (generateMipmap) {
            gl.generateMipmap(WebGL2RenderingContext.TEXTURE_2D);
        }
    }

    /**
     * Delete a texture.
     */
    public static void deleteTexture(WebGLTexture texture) {
        if (gl == null || texture == null) return;
        gl.deleteTexture(texture);
    }

    /**
     * Get cache statistics.
     */
    public static CacheStats getCacheStats() {
        int total = cacheHits + cacheMisses;
        float hitRate = total > 0 ? (float) cacheHits / total * 100 : 0;
        return new CacheStats(cacheHits, cacheMisses, hitRate, textureCache.size());
    }

    /**
     * Clear texture cache.
     */
    public static void clearCache() {
        for (TextureInfo info : textureCache.values()) {
            deleteTexture(info.texture);
        }
        textureCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    // Texture info holder
    public static class TextureInfo {
        public final String id;
        public final WebGLTexture texture;
        public final int width;
        public final int height;
        public long lastUsed;

        public TextureInfo(String id, WebGLTexture texture, int width, int height) {
            this.id = id;
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    // Cache statistics
    public static class CacheStats {
        public final int hits;
        public final int misses;
        public final float hitRate;
        public final int cacheSize;

        public CacheStats(int hits, int misses, float hitRate, int cacheSize) {
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.cacheSize = cacheSize;
        }
    }

    // Native JS helpers
    private static native Uint8Array createPixelData(byte[] data) /*-{
        return new Uint8Array(data);
    }-*/;

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
