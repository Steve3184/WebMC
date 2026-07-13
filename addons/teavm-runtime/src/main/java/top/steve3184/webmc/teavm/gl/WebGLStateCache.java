package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * WebGL state cache to minimize redundant state changes.
 * Tracks current OpenGL state and only issues commands when state changes.
 */
public final class WebGLStateCache {

    // Use Object for JS values that come from WebGL
    private static boolean depthTestEnabled = false;
    private static boolean blendEnabled = false;
    private static boolean cullFaceEnabled = false;
    private static int viewportX = 0, viewportY = 0;
    private static int viewportW = 0, viewportH = 0;
    private static int activeTextureUnit = 0;
    private static int boundArrayBuffer = 0;
    private static int boundElementArrayBuffer = 0;

    // Blend function state
    private static int blendSrcRgb = 0, blendDstRgb = 0;
    private static int blendSrcAlpha = 0, blendDstAlpha = 0;

    // Depth function
    private static int depthFunc = 0;

    private WebGLStateCache() {}

    /**
     * Initialize state cache with default values.
     */
    public static void init(WebGLRenderingContext gl) {
        depthTestEnabled = gl.isEnabled(WebGLRenderingContext.DEPTH_TEST);
        blendEnabled = gl.isEnabled(WebGLRenderingContext.BLEND);
        cullFaceEnabled = gl.isEnabled(WebGLRenderingContext.CULL_FACE);

        int[] vp = getViewport(gl);
        if (vp != null && vp.length >= 4) {
            viewportX = vp[0];
            viewportY = vp[1];
            viewportW = vp[2];
            viewportH = vp[3];
        }
    }

    @JSBody(params = {"gl"}, script =
        "try { var v = gl.getParameter(gl.VIEWPORT); return [v[0]|0, v[1]|0, v[2]|0, v[3]|0]; } catch(e) { return null; }"
    )
    private static native int[] getViewport(WebGLRenderingContext gl);

    /**
     * Enable/disable depth test with caching.
     */
    public static void setDepthTest(WebGLRenderingContext gl, boolean enabled) {
        if (depthTestEnabled != enabled) {
            if (enabled) {
                gl.enable(WebGLRenderingContext.DEPTH_TEST);
            } else {
                gl.disable(WebGLRenderingContext.DEPTH_TEST);
            }
            depthTestEnabled = enabled;
        }
    }

    /**
     * Set depth function.
     */
    public static void setDepthFunc(WebGLRenderingContext gl, int func) {
        if (depthFunc != func) {
            gl.depthFunc(func);
            depthFunc = func;
        }
    }

    /**
     * Enable/disable blending with caching.
     */
    public static void setBlend(WebGLRenderingContext gl, boolean enabled) {
        if (blendEnabled != enabled) {
            if (enabled) {
                gl.enable(WebGLRenderingContext.BLEND);
            } else {
                gl.disable(WebGLRenderingContext.BLEND);
            }
            blendEnabled = enabled;
        }
    }

    /**
     * Set blend function.
     */
    public static void setBlendFunc(WebGLRenderingContext gl, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        if (blendSrcRgb != srcRgb || blendDstRgb != dstRgb ||
            blendSrcAlpha != srcAlpha || blendDstAlpha != dstAlpha) {
            gl.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            blendSrcRgb = srcRgb;
            blendDstRgb = dstRgb;
            blendSrcAlpha = srcAlpha;
            blendDstAlpha = dstAlpha;
        }
    }

    /**
     * Enable/disable face culling with caching.
     */
    public static void setCullFace(WebGLRenderingContext gl, boolean enabled) {
        if (cullFaceEnabled != enabled) {
            if (enabled) {
                gl.enable(WebGLRenderingContext.CULL_FACE);
            } else {
                gl.disable(WebGLRenderingContext.CULL_FACE);
            }
            cullFaceEnabled = enabled;
        }
    }

    /**
     * Set cull face mode.
     */
    public static void setCullFaceMode(WebGLRenderingContext gl, int mode) {
        gl.cullFace(mode);
    }

    /**
     * Set viewport with caching.
     */
    public static void setViewport(WebGLRenderingContext gl, int x, int y, int w, int h) {
        if (viewportX != x || viewportY != y || viewportW != w || viewportH != h) {
            gl.viewport(x, y, w, h);
            viewportX = x;
            viewportY = y;
            viewportW = w;
            viewportH = h;
        }
    }

    /**
     * Active texture unit with caching.
     */
    public static void activeTexture(WebGLRenderingContext gl, int unit) {
        if (activeTextureUnit != unit) {
            gl.activeTexture(WebGLRenderingContext.TEXTURE0 + unit);
            activeTextureUnit = unit;
        }
    }

    /**
     * Bind array buffer with caching.
     */
    public static void bindArrayBuffer(WebGLRenderingContext gl, WebGLBuffer buffer) {
        int bufferId = buffer != null ? buffer.hashCode() : 0;
        if (boundArrayBuffer != bufferId) {
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer);
            boundArrayBuffer = bufferId;
        }
    }

    /**
     * Bind element array buffer with caching.
     */
    public static void bindElementArrayBuffer(WebGLRenderingContext gl, WebGLBuffer buffer) {
        int bufferId = buffer != null ? buffer.hashCode() : 0;
        if (boundElementArrayBuffer != bufferId) {
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buffer);
            boundElementArrayBuffer = bufferId;
        }
    }

    /**
     * Reset all cached state.
     */
    public static void reset() {
        depthTestEnabled = false;
        blendEnabled = false;
        cullFaceEnabled = false;
        viewportX = 0;
        viewportY = 0;
        viewportW = 0;
        viewportH = 0;
        activeTextureUnit = 0;
        boundArrayBuffer = 0;
        boundElementArrayBuffer = 0;
        blendSrcRgb = 0;
        blendDstRgb = 0;
        blendSrcAlpha = 0;
        blendDstAlpha = 0;
        depthFunc = 0;
    }
}
