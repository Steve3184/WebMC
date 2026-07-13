package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance texture manager with LRU caching and GPU compression hints.
 */
public final class TextureManager {

    private static TextureManager instance;

    private WebGLRenderingContext gl;
    private GpuDetector.GpuProfile profile;
    private Map<String, TextureInfo> textureCache;
    private int maxCacheSize;
    private int currentTextureCount = 0;
    private int textureMemoryUsedMB = 0;

    // Texture settings based on GPU tier
    private boolean useMipmaps = true;
    private int maxAnisotropy = 1;
    private int defaultFilter = WebGLRenderingContext.LINEAR_MIPMAP_LINEAR;

    public static class TextureInfo {
        public final String path;
        public final WebGLTexture texture;
        public final int width;
        public final int height;
        public final int format;
        private long lastUsed;
        public int refCount;

        public TextureInfo(String path, WebGLTexture texture, int width, int height, int format) {
            this.path = path;
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.format = format;
            this.lastUsed = System.currentTimeMillis();
            this.refCount = 1;
        }

        public void touch() {
            lastUsed = System.currentTimeMillis();
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public int getMemorySize() {
            // Estimate: RGBA = 4 bytes per pixel
            return width * height * 4 / (1024 * 1024);
        }
    }

    private TextureManager() {
        gl = WebGLContextHolder.gl();
        profile = GpuDetector.getProfile();
        textureCache = new HashMap<>();

        // Configure based on GPU tier
        configureForTier();
    }

    /**
     * Get singleton instance.
     */
    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    /**
     * Configure texture settings based on GPU tier.
     */
    private void configureForTier() {
        switch (profile.tier) {
            case ULTRA:
                maxCacheSize = 512;
                useMipmaps = true;
                maxAnisotropy = 16;
                defaultFilter = WebGLRenderingContext.LINEAR_MIPMAP_LINEAR;
                break;
            case HIGH:
                maxCacheSize = 256;
                useMipmaps = true;
                maxAnisotropy = 8;
                defaultFilter = WebGLRenderingContext.LINEAR_MIPMAP_LINEAR;
                break;
            case MEDIUM:
                maxCacheSize = 128;
                useMipmaps = true;
                maxAnisotropy = 4;
                defaultFilter = WebGLRenderingContext.LINEAR_MIPMAP_NEAREST;
                break;
            case LOW:
                maxCacheSize = 64;
                useMipmaps = false;
                maxAnisotropy = 1;
                defaultFilter = WebGLRenderingContext.NEAREST;
                break;
            default:
                maxCacheSize = 32;
                useMipmaps = false;
                maxAnisotropy = 1;
                defaultFilter = WebGLRenderingContext.NEAREST;
        }
        log("[TextureManager] Configured for " + profile.getTierName() +
             " tier: " + maxCacheSize + " textures, mipmaps=" + useMipmaps);
    }

    /**
     * Create a texture from raw pixel data.
     */
    public TextureInfo createTexture(String path, int width, int height, int[] pixels, int format) {
        if (gl == null) return null;

        // Check cache
        TextureInfo cached = textureCache.get(path);
        if (cached != null) {
            cached.touch();
            return cached;
        }

        // Evict if necessary
        while (textureCache.size() >= maxCacheSize) {
            evictLRU();
        }

        // Create texture
        WebGLTexture texture = gl.createTexture();
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);

        // Upload pixels using JS interop
        uploadTexture(gl, width, height, pixels, format);

        // Set filters
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
            WebGLRenderingContext.TEXTURE_MIN_FILTER, defaultFilter);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
            WebGLRenderingContext.TEXTURE_MAG_FILTER,
            WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
            WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.REPEAT);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
            WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.REPEAT);

        // Generate mipmaps if enabled
        if (useMipmaps) {
            gl.generateMipmap(WebGLRenderingContext.TEXTURE_2D);
        }

        // Create info
        TextureInfo info = new TextureInfo(path, texture, width, height, format);
        textureCache.put(path, info);
        currentTextureCount++;
        textureMemoryUsedMB += info.getMemorySize();

        log("[TextureManager] Created texture: " + path + " (" + width + "x" + height + ")");

        return info;
    }

    @JSBody(params = {"gl", "width", "height", "pixels", "format"}, script =
        "try {" +
        "  var arr = new Uint8Array(pixels.length * 4);" +
        "  for (var i = 0; i < pixels.length; i++) {" +
        "    arr[i * 4 + 0] = (pixels[i] >> 16) & 0xff;" +
        "    arr[i * 4 + 1] = (pixels[i] >> 8) & 0xff;" +
        "    arr[i * 4 + 2] = pixels[i] & 0xff;" +
        "    arr[i * 4 + 3] = (pixels[i] >> 24) & 0xff;" +
        "  }" +
        "  gl.texImage2D(gl.TEXTURE_2D, 0, format, width, height, 0, format, gl.UNSIGNED_BYTE, arr);" +
        "} catch(e) { console.error('texImage2D error:', e); }"
    )
    private static native void uploadTexture(WebGLRenderingContext gl, int width, int height, int[] pixels, int format);

    /**
     * Bind a texture.
     */
    public void bindTexture(TextureInfo info, int unit) {
        if (gl == null || info == null) return;

        gl.activeTexture(WebGLRenderingContext.TEXTURE0 + unit);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, info.texture);
        info.touch();
    }

    /**
     * Bind a texture by path (loads if necessary).
     */
    public void bindTexture(String path, int unit) {
        TextureInfo info = textureCache.get(path);
        if (info != null) {
            bindTexture(info, unit);
            return;
        }

        // Would need async loading - placeholder
        log("[TextureManager] Texture not loaded: " + path);
    }

    /**
     * Delete a texture.
     */
    public void deleteTexture(TextureInfo info) {
        if (gl == null || info == null) return;

        gl.deleteTexture(info.texture);
        textureCache.remove(info.path);
        currentTextureCount--;
        textureMemoryUsedMB -= info.getMemorySize();
    }

    /**
     * Evict least recently used texture.
     */
    private void evictLRU() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, TextureInfo> entry : textureCache.entrySet()) {
            if (entry.getValue().refCount == 0 && entry.getValue().getLastUsed() < oldestTime) {
                oldestTime = entry.getValue().getLastUsed();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            TextureInfo info = textureCache.get(oldestKey);
            if (info != null) {
                gl.deleteTexture(info.texture);
                textureCache.remove(oldestKey);
                currentTextureCount--;
                textureMemoryUsedMB -= info.getMemorySize();
                log("[TextureManager] Evicted: " + oldestKey);
            }
        }
    }

    /**
     * Clear all cached textures.
     */
    public void clearCache() {
        if (gl == null) return;

        for (TextureInfo info : textureCache.values()) {
            gl.deleteTexture(info.texture);
        }
        textureCache.clear();
        currentTextureCount = 0;
        textureMemoryUsedMB = 0;
        log("[TextureManager] Cache cleared");
    }

    /**
     * Get cached texture count.
     */
    public int getTextureCount() {
        return currentTextureCount;
    }

    /**
     * Get estimated memory usage in MB.
     */
    public int getMemoryUsageMB() {
        return textureMemoryUsedMB;
    }

    /**
     * Get max cache size.
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Check if mipmaps are enabled.
     */
    public boolean hasMipmaps() {
        return useMipmaps;
    }

    private static void log(String msg) {
        top.steve3184.webmc.teavm.WebLog.info(msg);
    }
}
