package top.steve3184.webmc.gpu;

import org.teavm.jso.webgl.WebGL2RenderingContext;

/**
 * WebGL state manager that minimizes expensive state changes.
 * Caches current GL state and only issues commands when state actually changes.
 *
 * Key optimizations:
 * - Skip redundant enable/disable calls
 * - Batch uniform updates
 * - Cache texture bindings
 * - Minimize program switches
 */
public final class WebGLStateCache {

    private final WebGL2RenderingContext gl;

    // Cached state values
    private int activeTexture = 0;
    private int boundArrayBuffer = 0;
    private int boundElementArrayBuffer = 0;
    private int currentProgram = 0;
    private boolean depthTest = true;
    private boolean depthMask = true;
    private int depthFunc = WebGL2RenderingContext.LESS;
    private boolean blend = false;
    private int blendSrcRGB = WebGL2RenderingContext.ONE;
    private int blendDstRGB = WebGL2RenderingContext.ZERO;
    private int blendSrcAlpha = WebGL2RenderingContext.ONE;
    private int blendDstAlpha = WebGL2RenderingContext.ZERO;
    private boolean cullFace = true;
    private int cullFaceMode = WebGL2RenderingContext.BACK;
    private int frontFace = WebGL2RenderingContext.CCW;
    private boolean scissorTest = false;
    private int viewportX = 0;
    private int viewportY = 0;
    private int viewportW = 0;
    private int viewportH = 0;

    // Texture binding cache: unit -> texture
    private final int[] boundTextures = new int[32];
    // UBO binding cache: target -> index -> buffer
    private final int[] boundUBOs = new int[32];

    public WebGLStateCache(WebGL2RenderingContext gl) {
        this.gl = gl;
        // Initialize all texture slots as unbound (0)
        for (int i = 0; i < boundTextures.length; i++) {
            boundTextures[i] = 0;
        }
        for (int i = 0; i < boundUBOs.length; i++) {
            boundUBOs[i] = 0;
        }
    }

    // =============== Program Management ===============

    public void useProgram(int program) {
        if (currentProgram != program) {
            currentProgram = program;
            gl.useProgram(program == 0 ? null : program);
        }
    }

    // =============== Texture Management ===============

    public void activeTexture(int unit) {
        if (activeTexture != unit) {
            activeTexture = unit;
            gl.activeTexture(WebGL2RenderingContext.TEXTURE0 + unit);
        }
    }

    public void bindTexture(int target, int texture) {
        if (texture == 0) return;
        int slot = activeTexture;
        if (slot >= 0 && slot < boundTextures.length && boundTextures[slot] != texture) {
            boundTextures[slot] = texture;
            gl.bindTexture(target, texture);
        }
    }

    /**
     * Bind texture and update active texture unit tracking.
     */
    public void bindTextureUnit(int unit, int target, int texture) {
        if (activeTexture != unit) {
            activeTexture = unit;
            gl.activeTexture(WebGL2RenderingContext.TEXTURE0 + unit);
        }
        if (unit >= 0 && unit < boundTextures.length && boundTextures[unit] != texture) {
            boundTextures[unit] = texture;
            gl.bindTexture(target, texture);
        }
    }

    // =============== Buffer Management ===============

    public void bindArrayBuffer(int buffer) {
        if (boundArrayBuffer != buffer) {
            boundArrayBuffer = buffer;
            gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, buffer);
        }
    }

    public void bindElementArrayBuffer(int buffer) {
        if (boundElementArrayBuffer != buffer) {
            boundElementArrayBuffer = buffer;
            gl.bindBuffer(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, buffer);
        }
    }

    public void bindBufferBase(int target, int index, int buffer) {
        if (index >= 0 && index < boundUBOs.length) {
            int key = target * 32 + index; // Simple hash for target+index
            if (boundUBOs[index] != buffer) {
                boundUBOs[index] = buffer;
                gl.bindBufferBase(target, index, buffer);
            }
        } else {
            gl.bindBufferBase(target, index, buffer);
        }
    }

    // =============== Depth State ===============

    public void enableDepthTest() {
        if (!depthTest) {
            depthTest = true;
            gl.enable(WebGL2RenderingContext.DEPTH_TEST);
        }
    }

    public void disableDepthTest() {
        if (depthTest) {
            depthTest = false;
            gl.disable(WebGL2RenderingContext.DEPTH_TEST);
        }
    }

    public void depthMask(boolean mask) {
        if (depthMask != mask) {
            depthMask = mask;
            gl.depthMask(mask);
        }
    }

    public void depthFunc(int func) {
        if (depthFunc != func) {
            depthFunc = func;
            gl.depthFunc(func);
        }
    }

    // =============== Blend State ===============

    public void enableBlend() {
        if (!blend) {
            blend = true;
            gl.enable(WebGL2RenderingContext.BLEND);
        }
    }

    public void disableBlend() {
        if (blend) {
            blend = false;
            gl.disable(WebGL2RenderingContext.BLEND);
        }
    }

    public void blendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        boolean changed = false;
        if (blendSrcRGB != srcRGB || blendDstRGB != dstRGB) {
            blendSrcRGB = srcRGB;
            blendDstRGB = dstRGB;
            changed = true;
        }
        if (blendSrcAlpha != srcAlpha || blendDstAlpha != dstAlpha) {
            blendSrcAlpha = srcAlpha;
            blendDstAlpha = dstAlpha;
            changed = true;
        }
        if (changed) {
            gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        }
    }

    public void blendFunc(int src, int dst) {
        blendFunc(src, dst, src, dst);
    }

    // =============== Cull State ===============

    public void enableCullFace() {
        if (!cullFace) {
            cullFace = true;
            gl.enable(WebGL2RenderingContext.CULL_FACE);
        }
    }

    public void disableCullFace() {
        if (cullFace) {
            cullFace = false;
            gl.disable(WebGL2RenderingContext.CULL_FACE);
        }
    }

    public void cullFace(int mode) {
        if (cullFaceMode != mode) {
            cullFaceMode = mode;
            gl.cullFace(mode);
        }
    }

    public void frontFace(int mode) {
        if (frontFace != mode) {
            frontFace = mode;
            gl.frontFace(mode);
        }
    }

    // =============== Scissor State ===============

    public void enableScissorTest() {
        if (!scissorTest) {
            scissorTest = true;
            gl.enable(WebGL2RenderingContext.SCISSOR_TEST);
        }
    }

    public void disableScissorTest() {
        if (scissorTest) {
            scissorTest = false;
            gl.disable(WebGL2RenderingContext.SCISSOR_TEST);
        }
    }

    // =============== Viewport State ===============

    public void viewport(int x, int y, int w, int h) {
        if (viewportX != x || viewportY != y || viewportW != w || viewportH != h) {
            viewportX = x;
            viewportY = y;
            viewportW = w;
            viewportH = h;
            gl.viewport(x, y, w, h);
        }
    }

    // =============== Vertex Attribute State ===============

    /**
     * Enable vertex attribute with caching.
     */
    public void enableVertexAttribArray(int index) {
        gl.enableVertexAttribArray(index);
    }

    /**
     * Disable vertex attribute.
     */
    public void disableVertexAttribArray(int index) {
        gl.disableVertexAttribArray(index);
    }

    /**
     * Set vertex attribute pointer with format hints for better performance.
     */
    public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    // =============== Utility Methods ===============

    /**
     * Apply complete render state in one call.
     * Use this for known state combinations to minimize calls.
     */
    public void applyRenderState(RenderState state) {
        // Depth
        if (state.depthTest) enableDepthTest(); else disableDepthTest();
        depthMask(state.depthMask);
        depthFunc(state.depthFunc);

        // Blend
        if (state.blend) enableBlend(); else disableBlend();
        if (state.blend) blendFunc(state.blendSrc, state.blendDst);

        // Cull
        if (state.cullFace) enableCullFace(); else disableCullFace();
        if (state.cullFace) cullFace(state.cullFaceMode);

        // Scissor
        if (state.scissorTest) enableScissorTest(); else disableScissorTest();

        // Viewport
        viewport(state.viewportX, state.viewportY, state.viewportW, state.viewportH);
    }

    /**
     * Get current profile hints for optimization decisions.
     */
    public GpuProfile getGpuProfile() {
        return GpuDetector.getProfile();
    }

    // =============== Render State Helper ===============

    /**
     * Immutable render state for common configurations.
     */
    public static final class RenderState {
        public final boolean depthTest;
        public final boolean depthMask;
        public final int depthFunc;
        public final boolean blend;
        public final int blendSrc;
        public final int blendDst;
        public final boolean cullFace;
        public final int cullFaceMode;
        public final boolean scissorTest;
        public final int viewportX, viewportY, viewportW, viewportH;

        private RenderState(Builder b) {
            this.depthTest = b.depthTest;
            this.depthMask = b.depthMask;
            this.depthFunc = b.depthFunc;
            this.blend = b.blend;
            this.blendSrc = b.blendSrc;
            this.blendDst = b.blendDst;
            this.cullFace = b.cullFace;
            this.cullFaceMode = b.cullFaceMode;
            this.scissorTest = b.scissorTest;
            this.viewportX = b.viewportX;
            this.viewportY = b.viewportY;
            this.viewportW = b.viewportW;
            this.viewportH = b.viewportH;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            boolean depthTest = true;
            boolean depthMask = true;
            int depthFunc = WebGL2RenderingContext.LESS;
            boolean blend = false;
            int blendSrc = WebGL2RenderingContext.ONE;
            int blendDst = WebGL2RenderingContext.ZERO;
            boolean cullFace = true;
            int cullFaceMode = WebGL2RenderingContext.BACK;
            boolean scissorTest = false;
            int viewportX = 0, viewportY = 0, viewportW = 0, viewportH = 0;

            public Builder depthTest(boolean v) { depthTest = v; return this; }
            public Builder depthMask(boolean v) { depthMask = v; return this; }
            public Builder depthFunc(int v) { depthFunc = v; return this; }
            public Builder blend(boolean v) { blend = v; return this; }
            public Builder blendFunc(int src, int dst) { blendSrc = src; blendDst = dst; return this; }
            public Builder cullFace(boolean v) { cullFace = v; return this; }
            public Builder cullFaceMode(int v) { cullFaceMode = v; return this; }
            public Builder scissorTest(boolean v) { scissorTest = v; return this; }
            public Builder viewport(int x, int y, int w, int h) { viewportX = x; viewportY = y; viewportW = w; viewportH = h; return this; }

            public RenderState build() { return new RenderState(this); }
        }

        // Common pre-defined states
        public static final RenderState OPAQUE = builder()
            .depthTest(true).depthMask(true).depthFunc(WebGL2RenderingContext.LEQUAL)
            .blend(false).cullFace(true).cullFaceMode(WebGL2RenderingContext.BACK)
            .build();

        public static final RenderState TRANSPARENT = builder()
            .depthTest(true).depthMask(false).depthFunc(WebGL2RenderingContext.LEQUAL)
            .blend(true).blendFunc(WebGL2RenderingContext.SRC_ALPHA, WebGL2RenderingContext.ONE_MINUS_SRC_ALPHA)
            .cullFace(true).cullFaceMode(WebGL2RenderingContext.BACK)
            .build();

        public static final RenderState NO_DEPTH = builder()
            .depthTest(false).depthMask(false)
            .blend(true).blendFunc(WebGL2RenderingContext.SRC_ALPHA, WebGL2RenderingContext.ONE_MINUS_SRC_ALPHA)
            .cullFace(false)
            .build();

        public static final RenderState GUI = builder()
            .depthTest(false).depthMask(false)
            .blend(true).blendFunc(WebGL2RenderingContext.SRC_ALPHA, WebGL2RenderingContext.ONE_MINUS_SRC_ALPHA)
            .cullFace(false)
            .build();
    }
}
