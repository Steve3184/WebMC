package top.steve3184.webmc.gpu;

import org.teavm.jso.JSObject;
import top.steve3184.webmc.teavm.gl.GpuDetector;

/**
 * WebGL state tracker that minimizes expensive state changes.
 * Tracks current GL state and provides hints about what needs to change.
 * Does not directly call GL methods - caller should check state and call GL as needed.
 *
 * Key optimizations:
 * - Skip redundant enable/disable calls
 * - Batch uniform updates
 * - Cache texture bindings
 * - Minimize program switches
 */
public final class WebGLStateCache {

    // Cached state values (tracked as JSObjects for TeaVM compatibility)
    private JSObject activeTexture = null;
    private JSObject boundArrayBuffer = null;
    private JSObject boundElementArrayBuffer = null;
    private JSObject currentProgram = null;
    private boolean depthTest = false;
    private boolean depthMask = true;
    private int depthFunc = 0x0202; // LESS
    private boolean blend = false;
    private int blendSrcRGB = 1; // ONE
    private int blendDstRGB = 0; // ZERO
    private int blendSrcAlpha = 1;
    private int blendDstAlpha = 0;
    private boolean cullFace = true;
    private int cullFaceMode = 0x0405; // BACK
    private int frontFace = 0x0901; // CCW
    private boolean scissorTest = false;
    private int viewportX = 0;
    private int viewportY = 0;
    private int viewportW = 0;
    private int viewportH = 0;

    // Texture binding cache: unit -> JSObject
    private final JSObject[] boundTextures = new JSObject[16];
    // UBO binding cache: unit -> JSObject
    private final JSObject[] boundUBOs = new JSObject[16];

    public WebGLStateCache() {
        // Initialize all texture slots as unbound
    }

    // =============== Program Management ===============

    /**
     * Check if program needs to be changed, returns true if different.
     */
    public boolean needsProgramSwitch(JSObject program) {
        return !jsEquals(currentProgram, program);
    }

    /**
     * Mark program as active.
     */
    public void setProgram(JSObject program) {
        currentProgram = program;
    }

    /**
     * Mark program as null (no program).
     */
    public void clearProgram() {
        currentProgram = null;
    }

    // =============== Texture Management ===============

    /**
     * Mark active texture unit.
     */
    public void setActiveTexture(int unit) {
        // Track unit number, caller calls gl.activeTexture
    }

    /**
     * Check if texture needs binding.
     */
    public boolean needsTextureBind(int unit, JSObject texture) {
        if (unit < 0 || unit >= boundTextures.length) return true;
        return !jsEquals(boundTextures[unit], texture);
    }

    /**
     * Mark texture as bound.
     */
    public void setBoundTexture(int unit, JSObject texture) {
        if (unit >= 0 && unit < boundTextures.length) {
            boundTextures[unit] = texture;
        }
    }

    // =============== Buffer Management ===============

    /**
     * Check if array buffer needs binding.
     */
    public boolean needsArrayBufferBind(JSObject buffer) {
        return !jsEquals(boundArrayBuffer, buffer);
    }

    /**
     * Mark array buffer as bound.
     */
    public void setBoundArrayBuffer(JSObject buffer) {
        boundArrayBuffer = buffer;
    }

    /**
     * Check if element array buffer needs binding.
     */
    public boolean needsElementArrayBufferBind(JSObject buffer) {
        return !jsEquals(boundElementArrayBuffer, buffer);
    }

    /**
     * Mark element array buffer as bound.
     */
    public void setBoundElementArrayBuffer(JSObject buffer) {
        boundElementArrayBuffer = buffer;
    }

    /**
     * Mark UBO as bound.
     */
    public void setBoundUBO(int unit, JSObject buffer) {
        if (unit >= 0 && unit < boundUBOs.length) {
            boundUBOs[unit] = buffer;
        }
    }

    // =============== Depth State ===============

    public boolean needsDepthTestEnable() { return !depthTest; }
    public boolean needsDepthTestDisable() { return depthTest; }
    public void setDepthTest(boolean enabled) { depthTest = enabled; }

    public boolean needsDepthMask(boolean mask) { return depthMask != mask; }
    public void setDepthMask(boolean mask) { depthMask = mask; }

    public boolean needsDepthFunc(int func) { return depthFunc != func; }
    public void setDepthFunc(int func) { depthFunc = func; }

    // =============== Blend State ===============

    public boolean needsBlendEnable() { return !blend; }
    public boolean needsBlendDisable() { return blend; }
    public void setBlend(boolean enabled) { blend = enabled; }

    public boolean needsBlendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        return blendSrcRGB != srcRGB || blendDstRGB != dstRGB ||
               blendSrcAlpha != srcAlpha || blendDstAlpha != dstAlpha;
    }

    public void setBlendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        blendSrcRGB = srcRGB;
        blendDstRGB = dstRGB;
        blendSrcAlpha = srcAlpha;
        blendDstAlpha = dstAlpha;
    }

    // =============== Cull State ===============

    public boolean needsCullFaceEnable() { return !cullFace; }
    public boolean needsCullFaceDisable() { return cullFace; }
    public void setCullFace(boolean enabled) { cullFace = enabled; }

    public boolean needsCullFaceMode(int mode) { return cullFaceMode != mode; }
    public void setCullFaceMode(int mode) { cullFaceMode = mode; }

    public boolean needsFrontFace(int mode) { return frontFace != mode; }
    public void setFrontFace(int mode) { frontFace = mode; }

    // =============== Scissor State ===============

    public boolean needsScissorTestEnable() { return !scissorTest; }
    public boolean needsScissorTestDisable() { return scissorTest; }
    public void setScissorTest(boolean enabled) { scissorTest = enabled; }

    // =============== Viewport State ===============

    public boolean needsViewport(int x, int y, int w, int h) {
        return viewportX != x || viewportY != y || viewportW != w || viewportH != h;
    }

    public void setViewport(int x, int y, int w, int h) {
        viewportX = x;
        viewportY = y;
        viewportW = w;
        viewportH = h;
    }

    // =============== Profile Hints ===============

    /**
     * Get current profile hints for optimization decisions.
     */
    public GpuProfile getGpuProfile() {
        return GpuDetector.getProfile();
    }

    /**
     * Reset all state tracking (e.g., after context loss).
     */
    public void reset() {
        activeTexture = null;
        boundArrayBuffer = null;
        boundElementArrayBuffer = null;
        currentProgram = null;
        depthTest = false;
        depthMask = true;
        blend = false;
        cullFace = true;
        scissorTest = false;
        for (int i = 0; i < boundTextures.length; i++) {
            boundTextures[i] = null;
        }
        for (int i = 0; i < boundUBOs.length; i++) {
            boundUBOs[i] = null;
        }
    }

    // TeaVM-safe JS object equality check
    private static native boolean jsEquals(JSObject a, JSObject b) /*-{
        return a === b;
    }-*/;
}
