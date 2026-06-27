package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLBuffer;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** WebGL2-backed {@link RenderPass}. Single global VAO, immediate-mode style. */
public final class WebRenderPass implements RenderPass {

    private static int drawCallCount = 0;
    private static int frameCount = 0;
    private static int setPipelineCount = 0;
    private static int setPipelineNullProgCount = 0;
    private static int setPipelineNullProgLogCount = 0;
    private static int setVertexBufCount = 0;
    private static int presentCount = 0;

    static void onPresent() {
        presentCount++;
        if (frameCount < 5 || frameCount % 60 == 0) {
            System.err.println("[mc-web/diag] frame=" + frameCount
                + " draws=" + drawCallCount
                + " setPipeline=" + setPipelineCount
                + "(nullProg=" + setPipelineNullProgCount + ")"
                + " setVB=" + setVertexBufCount
                + " setUniform=" + setUniformCount + "(miss=" + setUniformMissCount + ")"
                + " present=" + presentCount
                + " uploads=" + WebGpuBuffer.getUploadCount() + "/" + WebGpuBuffer.getUploadBytes() + "B");
        }
        drawCallCount = 0;
        setPipelineCount = 0;
        setPipelineNullProgCount = 0;
        setVertexBufCount = 0;
        setUniformCount = 0;
        setUniformMissCount = 0;
        presentCount = 0;
        frameCount++;
    }

    private final WebGpuDevice device;
    private final GpuTextureView colorAttachment;
    @Nullable private final GpuTextureView depthAttachment;
    private boolean closed = false;

    @Nullable private WebCompiledRenderPipeline currentPipeline;
    @Nullable private VertexFormat currentVertexFormat;
    @Nullable private WebGpuBuffer currentVertexBuffer;
    private final Map<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final Map<String, GpuTextureView> samplers = new HashMap<>();
    private int indexGlType = WebGL2RenderingContext.UNSIGNED_SHORT;

    public WebRenderPass(WebGpuDevice device, GpuTextureView color, @Nullable GpuTextureView depth) {
        this.device = device;
        this.colorAttachment = color;
        this.depthAttachment = depth;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        WebGpuTexture colorTex = (color != null && color.texture() instanceof WebGpuTexture wc) ? wc : null;
        WebGpuTexture depthTex = (depth != null && depth.texture() instanceof WebGpuTexture wd) ? wd : null;
        if (colorTex != null) {
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, colorTex.ensureFbo(gl, depthTex));
            gl.viewport(0, 0, colorTex.getWidth(0), colorTex.getHeight(0));
            if (frameCount < 3) {
                int status = gl.checkFramebufferStatus(WebGL2RenderingContext.FRAMEBUFFER);
                if (status != WebGL2RenderingContext.FRAMEBUFFER_COMPLETE) {
                    System.err.println("[mc-web/diag] FBO incomplete! status=0x" + Integer.toHexString(status)
                        + " color=" + colorTex.getWidth(0) + "x" + colorTex.getHeight(0)
                        + " fmt=" + colorTex.getFormat()
                        + " depth=" + (depthTex != null ? depthTex.getFormat() : "none"));
                }
            }
        } else if (depthTex != null) {
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, depthTex.ensureFbo(gl, null));
            gl.viewport(0, 0, depthTex.getWidth(0), depthTex.getHeight(0));
        } else {
            gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);
        }
    }

    @Override public void pushDebugGroup(Supplier<String> label) { /* no-op */ }
    @Override public void popDebugGroup()                        { /* no-op */ }

    @Override
    public void setPipeline(RenderPipeline p) {
        setPipelineCount++;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        WebCompiledRenderPipeline cp = device.lookupOrCompile(p);
        if (!cp.isValid() || cp.program() == null) {
            setPipelineNullProgCount++;
            if (setPipelineNullProgLogCount < 80) {
                setPipelineNullProgLogCount++;
                System.err.println("[mc-web/gl] setPipeline null program " + p.getLocation()
                    + " reason=" + cp.compileLog());
            }
            currentPipeline = null;
            return;
        }
        currentPipeline = cp;
        currentVertexFormat = cp.vertexFormat();
        gl.useProgram(cp.program());
        applyState(gl, p);
        // Pre-bind a placeholder UBO to every slot the program declares so
        // a forgotten setUniform doesn't crash with "used but unbound". MC
        // should still call setUniform for blocks it actually fills; this
        // only saves us when a block is declared by the shader but not
        // in MC's uniforms map (e.g. an optional conditional block).
        WebGLBuffer placeholder = device.uboPlaceholder();
        if (placeholder != null) {
            for (int slot : cp.allUboBindings()) {
                bindBufferBaseRaw(gl, WebGL2RenderingContext.UNIFORM_BUFFER, slot, placeholder);
            }
        }
        for (Map.Entry<String, GpuBufferSlice> entry : uniforms.entrySet()) {
            applyUniformSlice(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, GpuTextureView> entry : samplers.entrySet()) {
            applySampler(entry.getKey(), entry.getValue());
        }
    }

    private static void applyState(WebGL2RenderingContext gl, RenderPipeline p) {
        // Blend
        Optional<BlendFunction> blend = p.getBlendFunction();
        boolean blendingEnabled = blend.isPresent();
        if (blendingEnabled) {
            BlendFunction b = blend.get();
            gl.enable(WebGL2RenderingContext.BLEND);
            gl.blendFuncSeparate(srcFactor(b.sourceColor()), dstFactor(b.destColor()),
                                 srcFactor(b.sourceAlpha()), dstFactor(b.destAlpha()));
        } else {
            gl.disable(WebGL2RenderingContext.BLEND);
        }

        // Depth
        DepthTestFunction dt = p.getDepthTestFunction();
        if (dt == DepthTestFunction.NO_DEPTH_TEST) {
            gl.disable(WebGL2RenderingContext.DEPTH_TEST);
        } else {
            gl.enable(WebGL2RenderingContext.DEPTH_TEST);
            gl.depthFunc(depthFunc(dt));
        }
        // CRITICAL FIX: When blending is enabled (translucent materials like water/glass),
        // don't write to depth buffer. This prevents depth fighting artifacts where
        // translucent geometry incorrectly occludes or gets occluded by other geometry.
        // The shader should write the depth in the color buffer for later sorting,
        // but not update the depth buffer which is used for the depth test.
        if (blendingEnabled) {
            gl.depthMask(false);
        } else {
            gl.depthMask(p.isWriteDepth());
        }

        // Cull
        if (p.isCull()) {
            gl.enable(WebGL2RenderingContext.CULL_FACE);
            gl.cullFace(WebGL2RenderingContext.BACK);
            gl.frontFace(WebGL2RenderingContext.CCW);
        } else {
            gl.disable(WebGL2RenderingContext.CULL_FACE);
        }

        // Color/alpha mask
        gl.colorMask(p.isWriteColor(), p.isWriteColor(), p.isWriteColor(), p.isWriteAlpha());

        // Polygon offset
        float biasScale = p.getDepthBiasScaleFactor();
        float biasConst = p.getDepthBiasConstant();
        if (biasScale != 0.0f || biasConst != 0.0f) {
            gl.enable(WebGL2RenderingContext.POLYGON_OFFSET_FILL);
            gl.polygonOffset(biasScale, biasConst);
        } else {
            gl.disable(WebGL2RenderingContext.POLYGON_OFFSET_FILL);
        }
    }

    @Override
    public void bindSampler(String name, @Nullable GpuTextureView view) {
        if (view == null) {
            samplers.remove(name);
        } else {
            samplers.put(name, view);
        }
        applySampler(name, view);
    }

    private void applySampler(String name, @Nullable GpuTextureView view) {
        if (currentPipeline == null) return;
        Integer unit = currentPipeline.samplerUnitFor(name);
        if (unit == null) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.activeTexture(WebGL2RenderingContext.TEXTURE0 + unit);
        if (view == null) {
            gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, null);
            return;
        }
        if (view.texture() instanceof WebGpuTexture wt) {
            wt.ensureGlTexture(gl);
            gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, wt.webGlTexture());
        }
    }

    private static int setUniformCount = 0;
    private static int setUniformMissCount = 0;
    private static final int UBO_OFFSET_ALIGNMENT = 256;

    @Override
    public void setUniform(String name, GpuBuffer buf) {
        setUniform(name, buf.slice());
    }

    @Override
    public void setUniform(String name, GpuBufferSlice slice) {
        uniforms.put(name, slice);
        applyUniformSlice(name, slice);
    }

    private void applyUniformSlice(String name, GpuBufferSlice slice) {
        if (currentPipeline == null) return;
        Integer binding = currentPipeline.uboBindingFor(name);
        if (binding == null) {
            setUniformMissCount++;
            if (frameCount < 3) {
                System.err.println("[mc-web/diag] setUniform MISS name=" + name
                    + " pipeline=" + currentPipeline.pipeline().getLocation()
                    + " availableUBOs=" + currentPipeline.debugUboNames());
            }
            return;
        }
        if (!(slice.buffer() instanceof WebGpuBuffer wb)) return;
        setUniformCount++;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        int offset = slice.offset();
        int length = slice.length();
        Integer blockSize = currentPipeline.uboBlockSizeFor(name);
        if (frameCount < 3) {
            System.err.println("[mc-web/diag] setUniform name=" + name + " binding=" + binding
                + " blockSize=" + blockSize + " offset=" + offset + " length=" + length
                + " bufSize=" + wb.glAllocSize()
                + " pipeline=" + currentPipeline.pipeline().getLocation());
        }
        if (blockSize != null && blockSize > length) {
            int remaining = wb.glAllocSize() - offset;
            length = Math.min(blockSize, remaining);
        }
        if (isValidUboRange(offset, length, wb.glAllocSize())) {
            bindBufferRange(gl, WebGL2RenderingContext.UNIFORM_BUFFER, binding, wb, offset, length);
            return;
        }
        // WebGL2 rejects invalid UBO ranges (offset alignment / bounds) with
        // INVALID_VALUE. Fallback to base bind to keep draw path alive.
        bindBufferBase(gl, WebGL2RenderingContext.UNIFORM_BUFFER, binding, wb);
    }

    private static boolean isValidUboRange(int offset, int length, int allocSize) {
        if (offset < 0 || length <= 0) return false;
        if ((offset % UBO_OFFSET_ALIGNMENT) != 0) return false;
        if (offset > allocSize) return false;
        return length <= (allocSize - offset);
    }

    @org.teavm.jso.JSBody(params = {"gl", "target", "index", "buf"}, script =
        "gl.bindBufferBase(target, index, buf);")
    private static native void bindBufferBaseRaw(WebGL2RenderingContext gl, int target, int index, org.teavm.jso.webgl.WebGLBuffer buf);

    @org.teavm.jso.JSBody(params = {"gl", "target", "index", "buf", "off", "len"}, script =
        "gl.bindBufferRange(target, index, buf, off, len);")
    private static native void bindBufferRangeRaw(WebGL2RenderingContext gl, int target, int index, org.teavm.jso.webgl.WebGLBuffer buf, int off, int len);

    private static void bindBufferBase(WebGL2RenderingContext gl, int target, int index, WebGpuBuffer wb) {
        bindBufferBaseRaw(gl, target, index, wb.glHandle());
    }

    private static void bindBufferRange(WebGL2RenderingContext gl, int target, int index, WebGpuBuffer wb, int off, int len) {
        bindBufferRangeRaw(gl, target, index, wb.glHandle(), off, len);
    }

    @Override
    public void enableScissor(int x, int y, int w, int h) {
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.enable(WebGL2RenderingContext.SCISSOR_TEST);
        gl.scissor(x, y, w, h);
    }

    @Override
    public void disableScissor() {
        WebGLContextHolder.gl().disable(WebGL2RenderingContext.SCISSOR_TEST);
    }

    @Override
    public void setVertexBuffer(int slot, GpuBuffer buf) {
        setVertexBufCount++;
        if (currentPipeline == null || currentVertexFormat == null) return;
        if (!(buf instanceof WebGpuBuffer wb)) return;
        currentVertexBuffer = wb;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, wb.glHandle());
        applyVertexFormatPointers(gl, 0);
    }

    private void applyVertexFormatPointers(WebGL2RenderingContext gl, int baseVertex) {
        if (currentPipeline == null || currentVertexFormat == null) return;
        VertexFormat fmt = currentVertexFormat;
        int stride = fmt.getVertexSize();
        int baseOffset = baseVertex * stride;
        for (VertexFormatElement el : fmt.getElements()) {
            String name = fmt.getElementName(el);
            Integer loc = currentPipeline.attribLocation(name);
            if (loc == null || loc < 0) continue;
            int offset = baseOffset + fmt.getOffset(el);
            int glType = elementGlType(el.type());
            int count = el.count();
            if (useIntegerAttribPointer(el)) {
                vertexAttribIPointer(gl, loc, count, glType, stride, offset);
            } else {
                boolean normalized = elementNormalized(el);
                gl.vertexAttribPointer(loc, count, glType, normalized, stride, offset);
            }
            gl.enableVertexAttribArray(loc);
        }
    }

    @org.teavm.jso.JSBody(params = {"gl", "loc", "size", "type", "stride", "offset"}, script =
        "gl.vertexAttribIPointer(loc, size, type, stride, offset);")
    private static native void vertexAttribIPointer(WebGL2RenderingContext gl, int loc, int size, int type, int stride, int offset);

    @Override
    public void setIndexBuffer(GpuBuffer buf, VertexFormat.IndexType type) {
        if (!(buf instanceof WebGpuBuffer wb)) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.bindBuffer(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, wb.glHandle());
        indexGlType = (type == VertexFormat.IndexType.SHORT)
            ? WebGL2RenderingContext.UNSIGNED_SHORT
            : WebGL2RenderingContext.UNSIGNED_INT;
    }

    private static int diagDrawCount = 0;
    private static int invalidDrawArgsLogCount = 0;

    @Override
    public void drawIndexed(int baseVertex, int firstIndex, int indexCount, int instanceCount) {
        drawCallCount++;
        if (currentPipeline == null) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        if (currentVertexBuffer != null) {
            gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, currentVertexBuffer.glHandle());
            applyVertexFormatPointers(gl, baseVertex);
        }
        int mode = primitiveMode(currentPipeline.mode());
        int byteSize = (indexGlType == WebGL2RenderingContext.UNSIGNED_INT) ? 4 : 2;
        long offset = (long) firstIndex * byteSize;
        if (baseVertex < 0 || indexCount < 0 || instanceCount <= 0 || firstIndex < 0 || offset < 0 || offset > Integer.MAX_VALUE) {
            if (invalidDrawArgsLogCount < 8) {
                invalidDrawArgsLogCount++;
                System.err.println("[mc-web/diag] drawIndexed skip invalid args"
                    + " pipeline=" + currentPipeline.pipeline().getLocation()
                    + " firstIdx=" + firstIndex
                    + " baseVtx=" + baseVertex
                    + " idxCount=" + indexCount
                    + " instances=" + instanceCount
                    + " idxType=0x" + Integer.toHexString(indexGlType)
                    + " byteOffset=" + offset);
            }
            return;
        }

        // Detailed diagnostic on first 3 indexed draw calls
        if (diagDrawCount < 3) {
            diagDrawCount++;
            StringBuilder sb = new StringBuilder();
            sb.append("[mc-web/diag] drawIndexed #").append(diagDrawCount);
            sb.append(" pipeline=").append(currentPipeline.pipeline().getLocation());
            sb.append(" idxCount=").append(indexCount);
            sb.append(" firstIdx=").append(firstIndex);
            sb.append(" baseVtx=").append(baseVertex);
            sb.append(" instances=").append(instanceCount);
            sb.append(" mode=0x").append(Integer.toHexString(mode));
            sb.append(" idxType=0x").append(Integer.toHexString(indexGlType));
            if (currentVertexFormat != null) {
                sb.append(" attribs=[");
                for (VertexFormatElement el : currentVertexFormat.getElements()) {
                    String name = currentVertexFormat.getElementName(el);
                    Integer loc = currentPipeline.attribLocation(name);
                    sb.append(name).append("=").append(loc).append(" ");
                }
                sb.append("]");
            }
            sb.append(" ubos=").append(currentPipeline.debugUboNames());
            System.err.println(sb.toString());
        }

        // Before draw: dump buffer contents to verify data reached GPU
        if (diagDrawCount <= 3) {
            String vboStr = readVboFloats(gl, 8);
            String iboStr = readIboU16(gl, 12);
            String ubo0Str = readUboFloats(gl, 0, 16);
            String ubo1Str = readUboFloats(gl, 1, 16);

            System.err.println("[mc-web/diag] pre-draw #" + diagDrawCount
                + " VBO[0..7]=" + vboStr
                + " IBO[0..11]=" + iboStr);
            System.err.println("[mc-web/diag] pre-draw #" + diagDrawCount
                + " UBO0=" + ubo0Str);
            System.err.println("[mc-web/diag] pre-draw #" + diagDrawCount
                + " UBO1=" + ubo1Str);
        }

        if (instanceCount > 1) {
            gl.drawElementsInstanced(mode, indexCount, indexGlType, (int) offset, instanceCount);
        } else {
            gl.drawElements(mode, indexCount, indexGlType, (int) offset);
        }
        if (frameCount < 3) {
            int err = gl.getError();
            if (err != 0) {
                System.err.println("[mc-web/diag] drawIndexed GL error=0x" + Integer.toHexString(err)
                    + " pipeline=" + currentPipeline.pipeline().getLocation()
                    + " idxCount=" + indexCount + " mode=0x" + Integer.toHexString(mode));
            }
        }
        // After draw, read back center pixel to check if anything rendered
        if (diagDrawCount <= 3) {
            org.teavm.jso.typedarrays.Uint8Array px = org.teavm.jso.typedarrays.Uint8Array.create(4);
            readPixelAt(gl, 400, 300, px);
            System.err.println("[mc-web/diag] post-draw #" + diagDrawCount
                + " pixel@(400,300)=(" + px.get(0) + "," + px.get(1) + "," + px.get(2) + "," + px.get(3) + ")"
                + " viewport=" + getViewportString(gl)
                + " depthTest=" + gl.isEnabled(WebGL2RenderingContext.DEPTH_TEST)
                + " blend=" + gl.isEnabled(WebGL2RenderingContext.BLEND)
                + " scissor=" + gl.isEnabled(WebGL2RenderingContext.SCISSOR_TEST));
        }
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<Draw<T>> draws,
                                        @Nullable GpuBuffer ib,
                                        @Nullable VertexFormat.IndexType ibType,
                                        Collection<String> samplers,
                                        T uniformContext) {
        if (ib != null && ibType != null) setIndexBuffer(ib, ibType);
        for (Draw<T> d : draws) {
            setVertexBuffer(d.slot(), d.vertexBuffer());
            if (d.indexBuffer() != null && d.indexType() != null) {
                setIndexBuffer(d.indexBuffer(), d.indexType());
            }
            if (d.uniformUploaderConsumer() != null) {
                d.uniformUploaderConsumer().accept(uniformContext, this::uploadUniformSlice);
            }
            drawIndexed(0, d.firstIndex(), d.indexCount(), 1);
        }
    }

    private void uploadUniformSlice(String name, GpuBufferSlice slice) {
        setUniform(name, slice);
    }

    @Override
    public void draw(int firstVertex, int vertexCount) {
        drawCallCount++;
        if (currentPipeline == null) return;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();

        if (diagDrawCount < 3) {
            diagDrawCount++;
            StringBuilder sb = new StringBuilder();
            sb.append("[mc-web/diag] draw #").append(diagDrawCount);
            sb.append(" pipeline=").append(currentPipeline.pipeline().getLocation());
            sb.append(" vertCount=").append(vertexCount);
            sb.append(" first=").append(firstVertex);
            if (currentVertexFormat != null) {
                sb.append(" attribs=[");
                for (VertexFormatElement el : currentVertexFormat.getElements()) {
                    String name = currentVertexFormat.getElementName(el);
                    Integer loc = currentPipeline.attribLocation(name);
                    sb.append(name).append("=").append(loc).append(" ");
                }
                sb.append("]");
            }
            sb.append(" ubos=").append(currentPipeline.debugUboNames());
            sb.append(" writesColor=").append(currentPipeline.pipeline().isWriteColor());
            System.err.println(sb.toString());
        }

        gl.drawArrays(primitiveMode(currentPipeline.mode()), firstVertex, vertexCount);
        if (frameCount < 3) {
            int err = gl.getError();
            if (err != 0) {
                System.err.println("[mc-web/diag] draw GL error=0x" + Integer.toHexString(err)
                    + " pipeline=" + currentPipeline.pipeline().getLocation()
                    + " vertCount=" + vertexCount);
            }
        }
    }

    @Override public void close() {
        closed = true;
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        // Disable any per-attribute bindings to keep the shared default VAO clean.
        if (currentPipeline != null && currentVertexFormat != null) {
            for (VertexFormatElement el : currentVertexFormat.getElements()) {
                Integer loc = currentPipeline.attribLocation(currentVertexFormat.getElementName(el));
                if (loc != null && loc >= 0) gl.disableVertexAttribArray(loc);
            }
        }
        gl.useProgram(null);
    }

    // ---- enum mappings ----

    @org.teavm.jso.JSBody(params = {"gl", "x", "y", "px"}, script =
        "gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, px);")
    private static native void readPixelAt(WebGL2RenderingContext gl, int x, int y, org.teavm.jso.typedarrays.Uint8Array px);

    @org.teavm.jso.JSBody(params = {"gl"}, script =
        "var v = gl.getParameter(gl.VIEWPORT); return v[0]+','+v[1]+','+v[2]+','+v[3];")
    private static native String getViewportString(WebGL2RenderingContext gl);

    @org.teavm.jso.JSBody(params = {"gl", "count"}, script =
        "try { var d = new Float32Array(count); gl.getBufferSubData(gl.ARRAY_BUFFER, 0, d);" +
        " return '[' + Array.from(d).join(',') + ']'; } catch(e) { return '[err:'+e.message+']'; }")
    private static native String readVboFloats(WebGL2RenderingContext gl, int count);

    @org.teavm.jso.JSBody(params = {"gl", "count"}, script =
        "try { var d = new Uint16Array(count); gl.getBufferSubData(gl.ELEMENT_ARRAY_BUFFER, 0, d);" +
        " return '[' + Array.from(d).join(',') + ']'; } catch(e) { return '[err:'+e.message+']'; }")
    private static native String readIboU16(WebGL2RenderingContext gl, int count);

    @org.teavm.jso.JSBody(params = {"gl", "bindingIndex", "count"}, script =
        "try { var buf = gl.getIndexedParameter(gl.UNIFORM_BUFFER_BINDING, bindingIndex);" +
        " if (!buf) return '[no-buf]';" +
        " var prev = gl.getParameter(gl.UNIFORM_BUFFER_BINDING);" +
        " gl.bindBuffer(gl.UNIFORM_BUFFER, buf);" +
        " var d = new Float32Array(count); gl.getBufferSubData(gl.UNIFORM_BUFFER, 0, d);" +
        " gl.bindBuffer(gl.UNIFORM_BUFFER, prev);" +
        " return '[' + Array.from(d).join(',') + ']'; } catch(e) { return '[err:'+e.message+']'; }")
    private static native String readUboFloats(WebGL2RenderingContext gl, int bindingIndex, int count);

    private static int srcFactor(SourceFactor f) {
        switch (f) {
            case ZERO: return 0;
            case ONE: return 1;
            case SRC_COLOR: return 0x0300;
            case ONE_MINUS_SRC_COLOR: return 0x0301;
            case SRC_ALPHA: return 0x0302;
            case ONE_MINUS_SRC_ALPHA: return 0x0303;
            case DST_ALPHA: return 0x0304;
            case ONE_MINUS_DST_ALPHA: return 0x0305;
            case DST_COLOR: return 0x0306;
            case ONE_MINUS_DST_COLOR: return 0x0307;
            case SRC_ALPHA_SATURATE: return 0x0308;
            case CONSTANT_COLOR: return 0x8001;
            case ONE_MINUS_CONSTANT_COLOR: return 0x8002;
            case CONSTANT_ALPHA: return 0x8003;
            case ONE_MINUS_CONSTANT_ALPHA: return 0x8004;
            default: return 1;
        }
    }

    private static int dstFactor(DestFactor f) {
        switch (f) {
            case ZERO: return 0;
            case ONE: return 1;
            case SRC_COLOR: return 0x0300;
            case ONE_MINUS_SRC_COLOR: return 0x0301;
            case SRC_ALPHA: return 0x0302;
            case ONE_MINUS_SRC_ALPHA: return 0x0303;
            case DST_ALPHA: return 0x0304;
            case ONE_MINUS_DST_ALPHA: return 0x0305;
            case DST_COLOR: return 0x0306;
            case ONE_MINUS_DST_COLOR: return 0x0307;
            case CONSTANT_COLOR: return 0x8001;
            case ONE_MINUS_CONSTANT_COLOR: return 0x8002;
            case CONSTANT_ALPHA: return 0x8003;
            case ONE_MINUS_CONSTANT_ALPHA: return 0x8004;
            default: return 0;
        }
    }

    private static int depthFunc(DepthTestFunction d) {
        switch (d) {
            case EQUAL_DEPTH_TEST:   return 0x0202; // GL_EQUAL
            case LEQUAL_DEPTH_TEST:  return 0x0203; // GL_LEQUAL
            case LESS_DEPTH_TEST:    return 0x0201; // GL_LESS
            case GREATER_DEPTH_TEST: return 0x0204; // GL_GREATER
            default:                 return 0x0207; // GL_ALWAYS
        }
    }

    private static int primitiveMode(VertexFormat.Mode m) {
        switch (m) {
            case LINES:
            case DEBUG_LINES:        return 0x0001; // GL_LINES
            case LINE_STRIP:
            case DEBUG_LINE_STRIP:   return 0x0003; // GL_LINE_STRIP
            case TRIANGLES:
            case QUADS:              return 0x0004; // GL_TRIANGLES (quads use index buffer expansion)
            case TRIANGLE_STRIP:     return 0x0005;
            case TRIANGLE_FAN:       return 0x0006;
            default: return 0x0004;
        }
    }

    private static int elementGlType(VertexFormatElement.Type t) {
        switch (t) {
            case FLOAT:  return 0x1406;
            case UBYTE:  return 0x1401;
            case BYTE:   return 0x1400;
            case USHORT: return 0x1403;
            case SHORT:  return 0x1402;
            case UINT:   return 0x1405;
            case INT:    return 0x1404;
            default:     return 0x1406;
        }
    }

    private static boolean useIntegerAttribPointer(VertexFormatElement el) {
        return switch (el.usage()) {
            case POSITION, GENERIC, UV -> el.type() != VertexFormatElement.Type.FLOAT;
            case NORMAL, COLOR -> false;
        };
    }

    private static boolean elementNormalized(VertexFormatElement el) {
        return el.usage() == VertexFormatElement.Usage.NORMAL || el.usage() == VertexFormatElement.Usage.COLOR;
    }
}
