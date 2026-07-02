package org.lwjgl.opengl;

import top.steve3184.webmc.teavm.gl.GLBackend;
import top.steve3184.webmc.teavm.gl.GLBackendHolder;

import java.nio.ByteBuffer;

/**
 * Stub of {@code org.lwjgl.opengl.GL11}. Constants are spec-accurate. Function
 * bodies delegate to {@link GLBackend} or are no-ops where not yet wired.
 *
 * Add functions here as compile errors arise. Do NOT add functions from later
 * spec versions; put those in {@code GL12}..{@code GL33}.
 */
public final class GL11 {

    // ---- AccumOp (legacy, not used by 1.21 modern path) ----

    // ---- AlphaFunction ----
    public static final int GL_NEVER    = 0x0200;
    public static final int GL_LESS     = 0x0201;
    public static final int GL_EQUAL    = 0x0202;
    public static final int GL_LEQUAL   = 0x0203;
    public static final int GL_GREATER  = 0x0204;
    public static final int GL_NOTEQUAL = 0x0205;
    public static final int GL_GEQUAL   = 0x0206;
    public static final int GL_ALWAYS   = 0x0207;

    // ---- Boolean ----
    public static final int GL_FALSE = 0;
    public static final int GL_TRUE  = 1;

    // ---- BeginMode ----
    public static final int GL_POINTS         = 0x0000;
    public static final int GL_LINES          = 0x0001;
    public static final int GL_LINE_LOOP      = 0x0002;
    public static final int GL_LINE_STRIP     = 0x0003;
    public static final int GL_TRIANGLES      = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN   = 0x0006;
    public static final int GL_QUADS          = 0x0007; // legacy; emulated client-side

    // ---- BlendingFactor ----
    public static final int GL_ZERO                = 0;
    public static final int GL_ONE                 = 1;
    public static final int GL_SRC_COLOR           = 0x0300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    public static final int GL_SRC_ALPHA           = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA           = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_DST_COLOR           = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
    public static final int GL_SRC_ALPHA_SATURATE  = 0x0308;

    // ---- ClearBufferMask ----
    public static final int GL_DEPTH_BUFFER_BIT   = 0x00000100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
    public static final int GL_COLOR_BUFFER_BIT   = 0x00004000;

    // ---- DataType ----
    public static final int GL_BYTE           = 0x1400;
    public static final int GL_UNSIGNED_BYTE  = 0x1401;
    public static final int GL_SHORT          = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT            = 0x1404;
    public static final int GL_UNSIGNED_INT   = 0x1405;
    public static final int GL_FLOAT          = 0x1406;
    public static final int GL_2_BYTES        = 0x1407;
    public static final int GL_3_BYTES        = 0x1408;
    public static final int GL_4_BYTES        = 0x1409;
    public static final int GL_DOUBLE         = 0x140A;

    // ---- ErrorCode ----
    public static final int GL_NO_ERROR          = 0;
    public static final int GL_INVALID_ENUM      = 0x0500;
    public static final int GL_INVALID_VALUE     = 0x0501;
    public static final int GL_INVALID_OPERATION = 0x0502;
    public static final int GL_STACK_OVERFLOW    = 0x0503;
    public static final int GL_STACK_UNDERFLOW   = 0x0504;
    public static final int GL_OUT_OF_MEMORY     = 0x0505;

    // ---- FrontFaceDirection ----
    public static final int GL_CW  = 0x0900;
    public static final int GL_CCW = 0x0901;

    // ---- GetTarget / GetEnable ----
    public static final int GL_CULL_FACE                = 0x0B44;
    public static final int GL_DEPTH_TEST               = 0x0B71;
    public static final int GL_BLEND                    = 0x0BE2;
    public static final int GL_SCISSOR_TEST             = 0x0C11;
    public static final int GL_STENCIL_TEST             = 0x0B90;
    public static final int GL_TEXTURE_2D               = 0x0DE1;
    public static final int GL_VIEWPORT                 = 0x0BA2;
    public static final int GL_UNPACK_ALIGNMENT         = 0x0CF5;
    public static final int GL_PACK_ALIGNMENT           = 0x0D05;
    public static final int GL_UNPACK_ROW_LENGTH        = 0x0CF2;
    public static final int GL_VENDOR                   = 0x1F00;
    public static final int GL_RENDERER                 = 0x1F01;
    public static final int GL_VERSION                  = 0x1F02;
    public static final int GL_EXTENSIONS               = 0x1F03;

    // ---- HintMode ----
    public static final int GL_DONT_CARE = 0x1100;
    public static final int GL_FASTEST   = 0x1101;
    public static final int GL_NICEST    = 0x1102;

    // ---- PolygonMode ----
    public static final int GL_POINT = 0x1B00;
    public static final int GL_LINE  = 0x1B01;
    public static final int GL_FILL  = 0x1B02;

    // ---- TextureMagFilter / MinFilter ----
    public static final int GL_NEAREST                = 0x2600;
    public static final int GL_LINEAR                 = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST  = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR  = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR   = 0x2703;

    // ---- TextureWrapMode ----
    public static final int GL_REPEAT          = 0x2901;
    public static final int GL_CLAMP           = 0x2900; // legacy
    public static final int GL_TEXTURE_WRAP_S  = 0x2802;
    public static final int GL_TEXTURE_WRAP_T  = 0x2803;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;

    // ---- Pixel formats ----
    public static final int GL_RED             = 0x1903;
    public static final int GL_GREEN           = 0x1904;
    public static final int GL_BLUE            = 0x1905;
    public static final int GL_ALPHA           = 0x1906;
    public static final int GL_RGB             = 0x1907;
    public static final int GL_RGBA            = 0x1908;
    public static final int GL_LUMINANCE       = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;

    // ---- Cull face mode ----
    public static final int GL_FRONT          = 0x0404;
    public static final int GL_BACK           = 0x0405;
    public static final int GL_FRONT_AND_BACK = 0x0408;

    // ---- Functions (delegated) ----
    public static int  glGetError()                           { return GL_NO_ERROR; /* TODO via backend */ }
    public static String glGetString(int name)                { return GLBackendHolder.current().getString(name); }
    public static void glEnable(int cap)                      { GLBackendHolder.current().enable(cap); }
    public static void glDisable(int cap)                     { GLBackendHolder.current().disable(cap); }
    public static void glClear(int mask)                      { GLBackendHolder.current().clear(mask); }
    public static void glClearColor(float r, float g, float b, float a) { GLBackendHolder.current().clearColor(r, g, b, a); }
    public static void glClearDepth(double d)                 { GLBackendHolder.current().clearDepthf((float) d); }
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { GLBackendHolder.current().colorMask(r, g, b, a); }
    public static void glDepthMask(boolean flag)              { GLBackendHolder.current().depthMask(flag); }
    public static void glDepthFunc(int func)                  { GLBackendHolder.current().depthFunc(func); }
    public static void glBlendFunc(int src, int dst)          { GLBackendHolder.current().blendFunc(src, dst); }
    public static void glCullFace(int mode)                   { GLBackendHolder.current().cullFace(mode); }
    public static void glFrontFace(int mode)                  { GLBackendHolder.current().frontFace(mode); }
    public static void glViewport(int x, int y, int w, int h) { GLBackendHolder.current().viewport(x, y, w, h); }
    public static void glScissor(int x, int y, int w, int h)  { GLBackendHolder.current().scissor(x, y, w, h); }
    public static void glPolygonOffset(float factor, float units) { GLBackendHolder.current().polygonOffset(factor, units); }
    public static int  glGenTextures()                        { return GLBackendHolder.current().genTexture(); }
    public static void glDeleteTextures(int id)               { GLBackendHolder.current().deleteTexture(id); }
    public static void glBindTexture(int target, int id)      { GLBackendHolder.current().bindTexture(target, id); }
    public static void glTexParameteri(int t, int p, int v)   { GLBackendHolder.current().texParameteri(t, p, v); }
    public static void glTexParameterf(int t, int p, float v) { GLBackendHolder.current().texParameterf(t, p, v); }
    public static void glPixelStorei(int pname, int param)    { GLBackendHolder.current().pixelStorei(pname, param); }
    public static void glDrawArrays(int mode, int first, int count) { GLBackendHolder.current().drawArrays(mode, first, count); }
    public static void glDrawElements(int mode, int count, int type, long indices) { GLBackendHolder.current().drawElements(mode, count, type, indices); }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, ByteBuffer pixels) {
        GLBackendHolder.current().readPixels(x, y, w, h, format, type, pixels);
    }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, long pixelsPtr) { /* no-op */ }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, java.nio.IntBuffer pixels) { /* no-op */ }

    // glPolygonMode: WebGL2 has no equivalent. Tracked-as-state; ignored at draw time
    // (implemented by switching to GL_LINES emulation in the patched RenderType code).
    public static void glPolygonMode(int face, int mode) { /* no-op; see docs/lwjgl-mapping.md */ }

    // glTexImage2D with raw byte pointer is rare in modern MC; kept for completeness.
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height,
                                    int border, int format, int type, ByteBuffer pixels) {
        GLBackendHolder.current().texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height,
                                    int border, int format, int type, java.nio.IntBuffer pixels) {
        // pack IntBuffer into a backing ByteBuffer; we no-op since stubs don't actually upload
    }
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height,
                                    int border, int format, int type, long pixelsPtr) { /* no-op */ }

    public static void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height,
                                       int format, int type, ByteBuffer pixels) {
        GLBackendHolder.current().texSubImage2D(target, level, xoff, yoff, width, height, format, type, pixels);
    }
    public static void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height,
                                       int format, int type, java.nio.IntBuffer pixels) { /* no-op */ }
    public static void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height,
                                       int format, int type, long pixelsPtr) { /* no-op */ }

    public static int  glGetInteger(int pname)              { return 0; }
    public static void glGetIntegerv(int pname, int[] out)  { if (out != null && out.length > 0) out[0] = 0; }
    public static void glGetIntegerv(int pname, java.nio.IntBuffer out) { /* no-op */ }
    public static int  glGetTexLevelParameteri(int target, int level, int pname) { return 0; }
    public static void glDrawBuffer(int buf)                { /* WebGL2 only has drawBuffers; no-op for default */ }
    public static void glLogicOp(int op)                    { /* not in WebGL2 */ }

    private GL11() {}
}
