package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLTexture;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * GL-backed 2D texture. Handle is lazily created on first upload/bind via
 * {@link #ensureGlTexture(WebGL2RenderingContext)}. Immutable storage
 * (gl.texStorage2D) is allocated once, then mip levels filled via
 * texSubImage2D.
 *
 * Each color texture can also own a framebuffer object used when it is
 * attached as a render pass color target; {@link #ensureFbo} lazily
 * creates and configures the FBO, optionally re-attaching a depth texture.
 */
public final class WebGpuTexture extends GpuTexture {

    private boolean closed = false;
    private WebGLTexture glTex;
    private WebGLFramebuffer glFbo;
    private WebGpuTexture currentDepthAttachment;
    private boolean storageAllocated = false;

    public WebGpuTexture(int usage, String label, TextureFormat format,
                         int width, int height, int depthOrLayers, int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
    }

    /** Lazily create the GL texture and allocate immutable storage. */
    public void ensureGlTexture(WebGL2RenderingContext gl) {
        if (glTex != null) return;
        glTex = gl.createTexture();
        if (glTex == null) return;

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, glTex);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_MIN_FILTER,
                this.getMipLevels() > 1
                    ? WebGL2RenderingContext.NEAREST_MIPMAP_LINEAR
                    : WebGL2RenderingContext.NEAREST);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_MAG_FILTER,
                WebGL2RenderingContext.NEAREST);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_WRAP_S,
                WebGL2RenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_WRAP_T,
                WebGL2RenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_BASE_LEVEL, 0);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D,
                WebGL2RenderingContext.TEXTURE_MAX_LEVEL, this.getMipLevels() - 1);

        allocStorage(gl);
    }

    private void allocStorage(WebGL2RenderingContext gl) {
        if (storageAllocated) return;
        int internal = internalFormatFor(this.getFormat());
        texStorage2D(gl, WebGL2RenderingContext.TEXTURE_2D,
            this.getMipLevels(), internal, this.getWidth(0), this.getHeight(0));
        storageAllocated = true;
    }

    public WebGLTexture webGlTexture() { return glTex; }

    /**
     * Get or create an FBO with this texture attached (color or depth
     * depending on format). If {@code depth} is non-null and different from
     * the last bind, the depth attachment is (re)bound. Caller is responsible
     * for subsequently binding the returned FBO.
     */
    public WebGLFramebuffer ensureFbo(WebGL2RenderingContext gl, WebGpuTexture depth) {
        ensureGlTexture(gl);
        if (glFbo == null) {
            glFbo = gl.createFramebuffer();
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, glFbo);
            if (this.getFormat().hasColorAspect()) {
                gl.framebufferTexture2D(WebGL2RenderingContext.FRAMEBUFFER,
                    WebGL2RenderingContext.COLOR_ATTACHMENT0,
                    WebGL2RenderingContext.TEXTURE_2D, glTex, 0);
            } else if (this.getFormat().hasDepthAspect()) {
                gl.framebufferTexture2D(WebGL2RenderingContext.FRAMEBUFFER,
                    WebGL2RenderingContext.DEPTH_ATTACHMENT,
                    WebGL2RenderingContext.TEXTURE_2D, glTex, 0);
            }
        }
        if (depth != null) {
            depth.ensureGlTexture(gl);
            if (depth != currentDepthAttachment) {
                gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, glFbo);
                gl.framebufferTexture2D(WebGL2RenderingContext.FRAMEBUFFER,
                    WebGL2RenderingContext.DEPTH_ATTACHMENT,
                    WebGL2RenderingContext.TEXTURE_2D, depth.glTex, 0);
                currentDepthAttachment = depth;
            }
        } else if (currentDepthAttachment != null) {
            // Detach depth if depth becomes null (different from previous state)
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, glFbo);
            gl.framebufferTexture2D(WebGL2RenderingContext.FRAMEBUFFER,
                WebGL2RenderingContext.DEPTH_ATTACHMENT,
                WebGL2RenderingContext.TEXTURE_2D, null, 0);
            currentDepthAttachment = null;
        }
        return glFbo;
    }

    public static int internalFormatFor(TextureFormat fmt) {
        switch (fmt) {
            case RGBA8:   return 0x8058; // GL_RGBA8
            case RED8:    return 0x8229; // GL_R8
            case RED8I:   return 0x8231; // GL_R8I  (was incorrectly 0x8D94 = GL_RED_INTEGER, which is a format, not internal format)
            case DEPTH32: return 0x8CAC; // GL_DEPTH_COMPONENT32F
            default: return 0x8058;
        }
    }

    public static int dataFormatFor(TextureFormat fmt) {
        switch (fmt) {
            case RGBA8:   return 0x1908; // GL_RGBA
            case RED8:    return 0x1903; // GL_RED
            case RED8I:   return 0x8D94; // GL_RED_INTEGER
            case DEPTH32: return 0x1902; // GL_DEPTH_COMPONENT
            default: return 0x1908;
        }
    }

    public static int dataTypeFor(TextureFormat fmt) {
        switch (fmt) {
            case RGBA8:   return 0x1401; // GL_UNSIGNED_BYTE
            case RED8:    return 0x1401;
            case RED8I:   return 0x1400; // GL_BYTE
            case DEPTH32: return 0x1406; // GL_FLOAT
            default: return 0x1401;
        }
    }

    @org.teavm.jso.JSBody(params = {"gl", "target", "levels", "internal", "w", "h"}, script =
        "gl.texStorage2D(target, levels, internal, w, h);")
    private static native void texStorage2D(WebGL2RenderingContext gl, int target,
                                             int levels, int internal, int w, int h);

    @Override public void close() {
        if (closed) return;
        closed = true;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        if (gl != null) {
            if (glFbo != null) gl.deleteFramebuffer(glFbo);
            if (glTex != null) gl.deleteTexture(glTex);
        }
        glTex = null;
        glFbo = null;
    }

    @Override public boolean isClosed() { return closed; }
}
