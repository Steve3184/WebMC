package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;
import top.steve3184.webmc.teavm.WebLog;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * Texture manager for efficient texture handling.
 * Supports mipmapping, texture atlases, and GPU-tier specific optimizations.
 */
public final class TextureManager {

    private static TextureManager instance;

    private WebGLRenderingContext gl;
    private GpuDetector.GpuProfile profile;

    // Texture cache
    private WebGLTexture[] textureCache;
    private int textureCount = 0;
    private static final int MAX_TEXTURES = 256;

    // Active texture
    private WebGLTexture currentTexture;
    private int currentTextureUnit = 0;

    // Mipmap settings
    private boolean mipmapsEnabled = true;
    private int mipmapLevel = 0;

    // Anisotropic filtering
    private float maxAnisotropy = 1.0f;
    private boolean anisotropicEnabled = false;

    private TextureManager() {
        this.gl = WebGLContextHolder.gl();
        this.profile = GpuDetector.getProfile();
        this.textureCache = new WebGLTexture[MAX_TEXTURES];
    }

    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    /**
     * Initialize texture manager.
     */
    public void init() {
        if (gl == null) {
            WebLog.warn("[TextureManager] Cannot init: WebGL context not available");
            return;
        }

        // Setup texture quality based on GPU tier
        setupTextureQuality();

        WebLog.info("[TextureManager] Initialized (max textures: " + MAX_TEXTURES + ")");
        WebLog.info("[TextureManager] Anisotropic filtering: " +
                   (anisotropicEnabled ? maxAnisotropy + "x" : "disabled"));
    }

    private void setupTextureQuality() {
        switch (profile.tier) {
            case ULTRA:
                mipmapsEnabled = true;
                mipmapLevel = 4;
                anisotropicEnabled = true;
                maxAnisotropy = 8.0f;
                break;
            case HIGH:
                mipmapsEnabled = true;
                mipmapLevel = 3;
                anisotropicEnabled = true;
                maxAnisotropy = 4.0f;
                break;
            case MEDIUM:
                mipmapsEnabled = true;
                mipmapLevel = 2;
                anisotropicEnabled = true;
                maxAnisotropy = 2.0f;
                break;
            case LOW:
                mipmapsEnabled = false;
                mipmapLevel = 0;
                anisotropicEnabled = false;
                maxAnisotropy = 1.0f;
                break;
            default:
                mipmapsEnabled = true;
                mipmapLevel = 2;
                anisotropicEnabled = false;
        }
    }

    /**
     * Create a new texture.
     */
    public WebGLTexture createTexture(int width, int height) {
        WebGLTexture texture = gl.createTexture();
        if (texture == null) {
            WebLog.error("[TextureManager] Failed to create texture");
            return null;
        }

        bindTexture(texture, 0);

        // Allocate texture storage (1x1 placeholder)
        Int8Array data = Int8Array.create(width * height * 4);
        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                     WebGLRenderingContext.RGBA, width, height, 0,
                     WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, data);

        // Set default parameters
        setTextureParameters();

        // Cache texture
        if (textureCount < MAX_TEXTURES) {
            textureCache[textureCount++] = texture;
        }

        return texture;
    }

    /**
     * Upload texture data.
     */
    public void uploadTexture(WebGLTexture texture, int width, int height,
                             Int8Array data, boolean generateMipmaps) {
        bindTexture(texture, 0);

        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                     WebGLRenderingContext.RGBA, width, height, 0,
                     WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, data);

        if (generateMipmaps && mipmapsEnabled) {
            gl.generateMipmap(WebGLRenderingContext.TEXTURE_2D);
        }

        setTextureParameters();
    }

    /**
     * Bind texture to texture unit.
     */
    public void bindTexture(WebGLTexture texture, int unit) {
        if (unit != currentTextureUnit || texture != currentTexture) {
            gl.activeTexture(WebGLRenderingContext.TEXTURE0 + unit);
            gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
            currentTextureUnit = unit;
            currentTexture = texture;
        }
    }

    /**
     * Set texture parameters based on GPU tier.
     */
    private void setTextureParameters() {
        // Minification filter
        if (mipmapsEnabled) {
            switch (mipmapLevel) {
                case 4:
                case 3:
                    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                                    WebGLRenderingContext.TEXTURE_MIN_FILTER,
                                    WebGLRenderingContext.LINEAR_MIPMAP_LINEAR);
                    break;
                default:
                    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                                    WebGLRenderingContext.TEXTURE_MIN_FILTER,
                                    WebGLRenderingContext.LINEAR_MIPMAP_NEAREST);
            }
        } else {
            gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                            WebGLRenderingContext.TEXTURE_MIN_FILTER,
                            WebGLRenderingContext.LINEAR);
        }

        // Magnification filter
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                        WebGLRenderingContext.TEXTURE_MAG_FILTER,
                        WebGLRenderingContext.LINEAR);

        // Wrapping
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                        WebGLRenderingContext.TEXTURE_WRAP_S,
                        WebGLRenderingContext.REPEAT);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                        WebGLRenderingContext.TEXTURE_WRAP_T,
                        WebGLRenderingContext.REPEAT);
    }

    /**
     * Create a simple solid color texture.
     */
    public WebGLTexture createSolidColorTexture(float r, float g, float b, float a) {
        WebGLTexture texture = gl.createTexture();
        if (texture == null) return null;

        bindTexture(texture, 0);

        // Create 1x1 pixel texture using Int8Array
        Int8Array data = Int8Array.create(4);
        data.set(0, (byte)(int)(r * 255));
        data.set(1, (byte)(int)(g * 255));
        data.set(2, (byte)(int)(b * 255));
        data.set(3, (byte)(int)(a * 255));

        gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                     WebGLRenderingContext.RGBA, 1, 1, 0,
                     WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, data);

        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                        WebGLRenderingContext.TEXTURE_MIN_FILTER,
                        WebGLRenderingContext.NEAREST);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                        WebGLRenderingContext.TEXTURE_MAG_FILTER,
                        WebGLRenderingContext.NEAREST);

        return texture;
    }

    /**
     * Delete a texture.
     */
    public void deleteTexture(WebGLTexture texture) {
        if (texture != null) {
            gl.deleteTexture(texture);
        }
    }

    /**
     * Bind texture by ID.
     * Uses internal texture cache to find the texture.
     */
    public void bindTexture(int textureId) {
        if (textureId >= 0 && textureId < textureCount && textureCache[textureId] != null) {
            bindTexture(textureCache[textureId], 0);
        }
    }

    /**
     * Get current texture.
     */
    public WebGLTexture getCurrentTexture() {
        return currentTexture;
    }

    /**
     * Get texture count.
     */
    public int getTextureCount() {
        return textureCount;
    }

    /**
     * Clear texture cache.
     */
    public void clearCache() {
        for (int i = 0; i < textureCount; i++) {
            if (textureCache[i] != null) {
                gl.deleteTexture(textureCache[i]);
                textureCache[i] = null;
            }
        }
        textureCount = 0;
    }
}
