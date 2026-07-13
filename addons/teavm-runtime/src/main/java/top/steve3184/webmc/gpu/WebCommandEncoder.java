package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import org.teavm.jso.JSBody;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * CommandEncoder backed by WebGL2 immediate-mode calls. WebGL is not actually
 * a command-buffer API like Vulkan/Metal; we just translate calls eagerly.
 *
 * Phase 1: createRenderPass / createFence return real (no-op) objects so MC
 * can flow past the "begin pass" boundary; data ops still throw clearly.
 */
public final class WebCommandEncoder implements CommandEncoder {
    private final WebGpuDevice device;

    public WebCommandEncoder(WebGpuDevice device) { this.device = device; }

    @Override public RenderPass createRenderPass(Supplier<String> label, GpuTextureView color, OptionalInt clearColor) {
        WebRenderPass pass = new WebRenderPass(device, color, null);
        if (clearColor.isPresent()) {
            WebGL2RenderingContext gl = WebGLContextHolder.gl();
            int c = clearColor.getAsInt();
            float a = ((c >> 24) & 255) / 255.0f;
            float r = ((c >> 16) & 255) / 255.0f;
            float g = ((c >> 8)  & 255) / 255.0f;
            float b = (c & 255) / 255.0f;
            gl.colorMask(true, true, true, true);
            gl.clearColor(r, g, b, a);
            gl.clear(WebGL2RenderingContext.COLOR_BUFFER_BIT);
        }
        return pass;
    }
    @Override public RenderPass createRenderPass(Supplier<String> label, GpuTextureView color, OptionalInt clearColor,
                                                 @Nullable GpuTextureView depth, OptionalDouble clearDepth) {
        WebRenderPass pass = new WebRenderPass(device, color, depth);
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        int clearBits = 0;
        if (clearColor.isPresent()) {
            int c = clearColor.getAsInt();
            float a = ((c >> 24) & 255) / 255.0f;
            float r = ((c >> 16) & 255) / 255.0f;
            float g = ((c >> 8)  & 255) / 255.0f;
            float b = (c & 255) / 255.0f;
            gl.colorMask(true, true, true, true);
            gl.clearColor(r, g, b, a);
            clearBits |= WebGL2RenderingContext.COLOR_BUFFER_BIT;
        }
        if (clearDepth.isPresent()) {
            gl.depthMask(true);
            gl.clearDepth((float) clearDepth.getAsDouble());
            clearBits |= WebGL2RenderingContext.DEPTH_BUFFER_BIT;
        }
        if (clearBits != 0) gl.clear(clearBits);
        return pass;
    }

    @Override public void clearColorTexture(GpuTexture t, int color) {
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        bindTargetFbo(gl, t, null);
        setViewportFor(gl, t);
        float a = ((color >> 24) & 255) / 255.0f;
        float r = ((color >> 16) & 255) / 255.0f;
        float g = ((color >> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        gl.colorMask(true, true, true, true);
        gl.clearColor(r, g, b, a);
        gl.clear(WebGL2RenderingContext.COLOR_BUFFER_BIT);
    }
    @Override public void clearColorAndDepthTextures(GpuTexture c, int color, GpuTexture d, double depth) {
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        bindTargetFbo(gl, c, d);
        setViewportFor(gl, c);
        float a = ((color >> 24) & 255) / 255.0f;
        float r = ((color >> 16) & 255) / 255.0f;
        float g = ((color >> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        gl.colorMask(true, true, true, true);
        gl.depthMask(true);
        gl.clearColor(r, g, b, a);
        gl.clearDepth((float)depth);
        gl.clear(WebGL2RenderingContext.COLOR_BUFFER_BIT | WebGL2RenderingContext.DEPTH_BUFFER_BIT);
    }
    @Override public void clearColorAndDepthTextures(GpuTexture c, int color, GpuTexture d, double depth,
                                                     int x, int y, int w, int h) {
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.enable(WebGL2RenderingContext.SCISSOR_TEST);
        gl.scissor(x, y, w, h);
        clearColorAndDepthTextures(c, color, d, depth);
        gl.disable(WebGL2RenderingContext.SCISSOR_TEST);
    }
    @Override public void clearDepthTexture(GpuTexture t, double depth) {
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        bindTargetFbo(gl, null, t);
        setViewportFor(gl, t);
        gl.depthMask(true);
        gl.clearDepth((float)depth);
        gl.clear(WebGL2RenderingContext.DEPTH_BUFFER_BIT);
    }

    /**
     * Bind the FBO whose color attachment is {@code color} (or whose depth
     * attachment is {@code depth} if {@code color} is null). Null color with
     * null depth binds the default framebuffer.
     */
    private static void bindTargetFbo(WebGL2RenderingContext gl, GpuTexture color, GpuTexture depth) {
        if (color instanceof WebGpuTexture wc) {
            WebGpuTexture wd = (depth instanceof WebGpuTexture) ? (WebGpuTexture) depth : null;
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, wc.ensureFbo(gl, wd));
        } else if (depth instanceof WebGpuTexture wd) {
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, wd.ensureFbo(gl, null));
        } else {
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);
        }
    }

    private static void setViewportFor(WebGL2RenderingContext gl, GpuTexture t) {
        if (t != null) gl.viewport(0, 0, t.getWidth(0), t.getHeight(0));
    }

    private static int oobLogCount = 0;
    private static boolean checkBounds(String tag, GpuTexture tex, int mipLevel, int xOff, int yOff, int srcW, int srcH) {
        int tw = tex.getWidth(mipLevel);
        int th = tex.getHeight(mipLevel);
        if (mipLevel < 0 || mipLevel >= tex.getMipLevels()
                || xOff < 0 || yOff < 0 || srcW < 0 || srcH < 0
                || xOff + srcW > tw || yOff + srcH > th) {
            if (oobLogCount < 8) {
                oobLogCount++;
                System.err.println("[mc-web/gl] writeToTexture OOB " + tag
                        + " tex=" + tex.getWidth(0) + "x" + tex.getHeight(0)
                        + " mips=" + tex.getMipLevels()
                        + " level=" + mipLevel + " levelDim=" + tw + "x" + th
                        + " dst=(" + xOff + "," + yOff + " " + srcW + "x" + srcH + ")");
            }
            return false;
        }
        return true;
    }

    private static int writeToBufferCount = 0;
    private static int mapBufferCount = 0;

    @Override public void writeToBuffer(GpuBufferSlice s, ByteBuffer b) {
        writeToBufferCount++;
        if (writeToBufferCount <= 20) {
            String targetName = "?";
            if (s.buffer() instanceof WebGpuBuffer wb) {
                targetName = (wb.glTarget() == WebGL2RenderingContext.ARRAY_BUFFER) ? "VBO"
                    : (wb.glTarget() == WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER) ? "IBO"
                    : (wb.glTarget() == WebGL2RenderingContext.UNIFORM_BUFFER) ? "UBO" : "?";
            }
            // Dump first 16 bytes as floats for UBO, as bytes otherwise
            StringBuilder preview = new StringBuilder();
            if (b != null && b.remaining() > 0) {
                ByteBuffer peek = b.duplicate();
                if (targetName.equals("UBO") && peek.remaining() >= 4) {
                    int nf = Math.min(peek.remaining() / 4, 8);
                    for (int i = 0; i < nf; i++) {
                        if (i > 0) preview.append(",");
                        preview.append(String.format("%.4f", peek.getFloat()));
                    }
                } else {
                    int nb = Math.min(peek.remaining(), 16);
                    for (int i = 0; i < nb; i++) {
                        if (i > 0) preview.append(",");
                        preview.append(peek.get() & 0xFF);
                    }
                }
            }
            System.err.println("[mc-web/diag] writeToBuffer #" + writeToBufferCount
                + " target=" + targetName
                + " offset=" + s.offset() + " len=" + s.length()
                + " remaining=" + (b != null ? b.remaining() : "null")
                + " data=[" + preview + "]");
        }
        if (s.buffer() instanceof WebGpuBuffer wb) wb.uploadAt(s.offset(), b);
        else throw u("writeToBuffer (non-Web buffer)");
    }
    @Override public GpuBuffer.MappedView mapBuffer(GpuBuffer buf, boolean read, boolean write) {
        mapBufferCount++;
        if (mapBufferCount <= 5) {
            System.err.println("[mc-web/diag] mapBuffer #" + mapBufferCount
                + " size=" + buf.size() + " read=" + read + " write=" + write
                + " bufType=" + buf.getClass().getSimpleName());
        }
        if (buf instanceof WebGpuBuffer wb) {
            return new WebGpuBuffer.WebMappedView(wb, 0, wb.size(), wb.mappedBuffer(0, wb.size()));
        }
        return new WebGpuBuffer.WebMappedView(java.nio.ByteBuffer.allocate(buf.size()));
    }
    @Override public GpuBuffer.MappedView mapBuffer(GpuBufferSlice s, boolean read, boolean write) {
        mapBufferCount++;
        if (mapBufferCount <= 5) {
            System.err.println("[mc-web/diag] mapBufferSlice #" + mapBufferCount
                + " offset=" + s.offset() + " len=" + s.length()
                + " read=" + read + " write=" + write);
        }
        if (s.buffer() instanceof WebGpuBuffer wb) {
            return new WebGpuBuffer.WebMappedView(wb, s.offset(), s.length(), wb.mappedBuffer(s.offset(), s.length()));
        }
        return new WebGpuBuffer.WebMappedView(java.nio.ByteBuffer.allocate(s.length()));
    }
    @Override public void copyToBuffer(GpuBufferSlice src, GpuBufferSlice dst)                                { throw u("copyToBuffer"); }
    @Override public void writeToTexture(GpuTexture tex, NativeImage img) {
        if (!(tex instanceof WebGpuTexture wt)) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        wt.ensureGlTexture(gl);

        int w = img.getWidth();
        int h = img.getHeight();
        if (!checkBounds("full", tex, 0, 0, 0, w, h)) return;
        int pixelCount = w * h;

        int[] abgrPixels = img.getPixelsABGR();
        org.teavm.jso.typedarrays.Uint8Array jsArr = org.teavm.jso.typedarrays.Uint8Array.create(pixelCount * 4);
        swizzleABGRtoRGBA(abgrPixels, jsArr);

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, wt.webGlTexture());
        texSubImage2D(gl, 0, 0, 0, w, h,
            WebGpuTexture.dataFormatFor(tex.getFormat()),
            WebGpuTexture.dataTypeFor(tex.getFormat()),
            jsArr);
    }

    @JSBody(params = {"src", "dst"}, script =
        "var src32 = new Uint32Array(src.buffer, src.byteOffset, src.length);" +
        "var dst32 = new Uint32Array(dst.buffer, dst.byteOffset, src.length);" +
        "for (var i = 0; i < src32.length; i++) {" +
        "  var p = src32[i];" +
        "  dst32[i] = (p & 0xFF00FF00) | ((p & 0x00FF0000) >> 16) | ((p & 0x000000FF) << 16);" +
        "}")
    private static native void swizzleABGRtoRGBA(int[] src, org.teavm.jso.typedarrays.Uint8Array dst);

    @Override public void writeToTexture(GpuTexture tex, NativeImage img,
                                         int mipLevel, int depthOrLayer,
                                         int xOff, int yOff, int srcW, int srcH, int srcX, int srcY) {
        if (!(tex instanceof WebGpuTexture wt)) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        wt.ensureGlTexture(gl);
        if (!checkBounds("sub", tex, mipLevel, xOff, yOff, srcW, srcH)) return;

        int[] abgrPixels = img.getPixelsABGR();
        int imgW = img.getWidth();

        org.teavm.jso.typedarrays.Uint8Array dst = org.teavm.jso.typedarrays.Uint8Array.create(srcW * srcH * 4);
        extractSubregionABGRtoRGBA(abgrPixels, imgW, srcX, srcY, srcW, srcH, dst);

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, wt.webGlTexture());
        texSubImage2D(gl, mipLevel, xOff, yOff, srcW, srcH,
            WebGpuTexture.dataFormatFor(tex.getFormat()),
            WebGpuTexture.dataTypeFor(tex.getFormat()),
            dst);
    }

    @JSBody(params = {"src", "imgW", "sx", "sy", "sw", "sh", "dst"}, script =
        "var src32 = new Uint32Array(src.buffer, src.byteOffset, src.length);" +
        "var dst32 = new Uint32Array(dst.buffer, dst.byteOffset, sw * sh);" +
        "var di = 0;" +
        "for (var y = 0; y < sh; y++) {" +
        "  var row = (sy + y) * imgW + sx;" +
        "  for (var x = 0; x < sw; x++) {" +
        "    var p = src32[row + x];" +
        "    dst32[di++] = (p & 0xFF00FF00) | ((p & 0x00FF0000) >> 16) | ((p & 0x000000FF) << 16);" +
        "  }" +
        "}")
    private static native void extractSubregionABGRtoRGBA(int[] src, int imgW, int sx, int sy, int sw, int sh, org.teavm.jso.typedarrays.Uint8Array dst);

    @Override public void writeToTexture(GpuTexture tex, IntBuffer pix, NativeImage.Format fmt,
                                         int mipLevel, int arg4, int xOff, int yOff, int srcW, int srcH) {
        if (!(tex instanceof WebGpuTexture wt) || pix == null) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        wt.ensureGlTexture(gl);
        if (!checkBounds("ibuf", tex, mipLevel, xOff, yOff, srcW, srcH)) return;

        int n = srcW * srcH;
        org.teavm.jso.typedarrays.Uint8Array dst = org.teavm.jso.typedarrays.Uint8Array.create(n * 4);
        int basePos = pix.position();
        int[] copy = new int[n];
        for (int i = 0; i < n; i++) copy[i] = pix.get(basePos + i);
        swizzleABGRtoRGBA(copy, dst);

        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, wt.webGlTexture());
        texSubImage2D(gl, mipLevel, xOff, yOff, srcW, srcH,
            WebGpuTexture.dataFormatFor(tex.getFormat()),
            WebGpuTexture.dataTypeFor(tex.getFormat()),
            dst);
    }
    @Override public void copyTextureToBuffer(GpuTexture tex, GpuBuffer buf, int o, Runnable cb, int idx)     { if (cb != null) cb.run(); /* phase-3 */ }
    @Override public void copyTextureToBuffer(GpuTexture tex, GpuBuffer buf, int o, Runnable cb, int idx,
                                              int a, int b, int c, int d)                                    { if (cb != null) cb.run(); /* phase-3 */ }
    @Override public void copyTextureToTexture(GpuTexture src, GpuTexture dst,
                                               int mipLevel, int dstX, int dstY,
                                               int srcX, int srcY, int width, int height) {
        if (!(src instanceof WebGpuTexture ws) || !(dst instanceof WebGpuTexture wd)) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        ws.ensureGlTexture(gl);
        wd.ensureGlTexture(gl);
        gl.bindFramebuffer(WebGL2RenderingContext.READ_FRAMEBUFFER, ws.ensureFbo(gl, null));
        gl.bindFramebuffer(WebGL2RenderingContext.DRAW_FRAMEBUFFER, wd.ensureFbo(gl, null));
        boolean isDepth = src.getFormat().hasDepthAspect();
        int mask = isDepth ? WebGL2RenderingContext.DEPTH_BUFFER_BIT : WebGL2RenderingContext.COLOR_BUFFER_BIT;
        blitFramebufferMask(gl, srcX, srcY, srcX + width, srcY + height,
                            dstX, dstY, dstX + width, dstY + height, mask);
        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);
    }
    @Override public void presentTexture(GpuTextureView view) {
        WebRenderPass.onPresent();
        if (view == null) return;
        if (!(view.texture() instanceof WebGpuTexture wt)) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        int w = wt.getWidth(0);
        int h = wt.getHeight(0);

        // Get canvas size before binding any framebuffer
        int canvasW = getDrawingBufferWidth(gl);
        int canvasH = getDrawingBufferHeight(gl);
        if (canvasW <= 0 || canvasH <= 0) {
            canvasW = w;
            canvasH = h;
        }

        // Read FBO center pixel for diagnostics
        if (presentDiagCount < 5) {
            presentDiagCount++;
            gl.bindFramebuffer(WebGL2RenderingContext.READ_FRAMEBUFFER, wt.ensureFbo(gl, null));
            org.teavm.jso.typedarrays.Uint8Array px = org.teavm.jso.typedarrays.Uint8Array.create(4);
            // Note: readPixels y is from bottom, so we need to flip
            readPixels(gl, w / 2, h / 2, 1, 1, px);
            int errBefore = gl.getError();
            System.err.println("[mc-web/diag] presentTexture: fbo=" + w + "x" + h
                + " canvas=" + canvasW + "x" + canvasH
                + " centerPx=(" + getU8(px, 0) + "," + getU8(px, 1) + "," + getU8(px, 2) + "," + getU8(px, 3) + ")"
                + " errBefore=0x" + Integer.toHexString(errBefore));
        }

        // Blit from FBO to default framebuffer (canvas)
        // Note: blitFramebuffer flips Y axis automatically when writing to default framebuffer
        // This matches the expected behavior where (0,0) in canvas is top-left
        gl.bindFramebuffer(WebGL2RenderingContext.READ_FRAMEBUFFER, wt.ensureFbo(gl, null));
        gl.bindFramebuffer(WebGL2RenderingContext.DRAW_FRAMEBUFFER, null);
        gl.colorMask(true, true, true, true);
        gl.disable(WebGL2RenderingContext.SCISSOR_TEST);

        viewport(gl, 0, 0, canvasW, canvasH);

        // Blit with Y-flip: source (0,0) is bottom-left, dest (0,0) is bottom-left
        // But canvas uses top-left as origin, so we need to flip the Y in the destination
        // WebGL blitFramebuffer handles this automatically when one target is the default framebuffer
        blitFramebuffer(gl, 0, 0, w, h, 0, canvasH, canvasW, 0);

        if (presentDiagCount <= 5) {
            int errAfter = gl.getError();
            if (errAfter != 0) {
                System.err.println("[mc-web/diag] presentTexture blit GL error=0x" + Integer.toHexString(errAfter)
                    + " src=" + w + "x" + h + " dst=" + canvasW + "x" + canvasH);
            }
        }
        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);
    }

    private static int presentDiagCount = 0;

    @JSBody(params = "gl", script = "return gl.drawingBufferWidth;")
    private static native int getDrawingBufferWidth(WebGL2RenderingContext gl);
    @JSBody(params = "gl", script = "return gl.drawingBufferHeight;")
    private static native int getDrawingBufferHeight(WebGL2RenderingContext gl);
    @JSBody(params = {"gl", "x", "y", "w", "h", "dst"}, script =
        "gl.readPixels(x, y, w, h, gl.RGBA, gl.UNSIGNED_BYTE, dst);")
    private static native void readPixels(WebGL2RenderingContext gl, int x, int y, int w, int h, org.teavm.jso.typedarrays.Uint8Array dst);
    @JSBody(params = {"arr", "i"}, script = "return arr[i];")
    private static native int getU8(org.teavm.jso.typedarrays.Uint8Array arr, int i);

    @JSBody(params = {"gl", "sx0", "sy0", "sx1", "sy1", "dx0", "dy0", "dx1", "dy1"}, script =
        "gl.blitFramebuffer(sx0, sy0, sx1, sy1, dx0, dy0, dx1, dy1, gl.COLOR_BUFFER_BIT, gl.NEAREST);")
    private static native void blitFramebuffer(WebGL2RenderingContext gl, int sx0, int sy0, int sx1, int sy1, int dx0, int dy0, int dx1, int dy1);

    @JSBody(params = {"gl", "x", "y", "w", "h"}, script =
        "gl.viewport(x, y, w, h);")
    private static native void viewport(WebGL2RenderingContext gl, int x, int y, int w, int h);

    @JSBody(params = {"gl", "sx0", "sy0", "sx1", "sy1", "dx0", "dy0", "dx1", "dy1", "mask"}, script =
        "gl.blitFramebuffer(sx0, sy0, sx1, sy1, dx0, dy0, dx1, dy1, mask, gl.NEAREST);")
    private static native void blitFramebufferMask(WebGL2RenderingContext gl, int sx0, int sy0, int sx1, int sy1, int dx0, int dy0, int dx1, int dy1, int mask);
    @Override public GpuFence createFence() { return new WebGpuFence(); }

    @JSBody(params = {"gl", "level", "x", "y", "w", "h", "fmt", "type", "data"}, script =
        "gl.texSubImage2D(gl.TEXTURE_2D, level, x, y, w, h, fmt, type, data);")
    private static native void texSubImage2D(WebGL2RenderingContext gl, int level, int x, int y, int w, int h, int fmt, int type,
                                              org.teavm.jso.typedarrays.Uint8Array data);

    private static UnsupportedOperationException u(String what) {
        return new UnsupportedOperationException("WebCommandEncoder." + what + " — TODO phase 3");
    }
}
