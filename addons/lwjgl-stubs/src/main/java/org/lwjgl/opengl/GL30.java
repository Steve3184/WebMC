package org.lwjgl.opengl;

import top.steve3184.webmc.teavm.gl.GLBackendHolder;
import java.nio.IntBuffer;

/** Stub of {@code org.lwjgl.opengl.GL30}. VAO + FBO + RBO + integer formats + texture arrays. */
public final class GL30 {
    // VAO
    public static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;

    // FBO
    public static final int GL_FRAMEBUFFER          = 0x8D40;
    public static final int GL_DRAW_FRAMEBUFFER     = 0x8CA9;
    public static final int GL_READ_FRAMEBUFFER     = 0x8CA8;
    public static final int GL_COLOR_ATTACHMENT0    = 0x8CE0;
    public static final int GL_COLOR_ATTACHMENT1    = 0x8CE1;
    public static final int GL_DEPTH_ATTACHMENT     = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT   = 0x8D20;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;

    // RBO
    public static final int GL_RENDERBUFFER         = 0x8D41;
    public static final int GL_DEPTH_COMPONENT24    = 0x81A6;
    public static final int GL_DEPTH_COMPONENT32F   = 0x8CAC;
    public static final int GL_DEPTH24_STENCIL8     = 0x88F0;
    public static final int GL_DEPTH32F_STENCIL8    = 0x8CAD;

    // Sized internal formats (subset used by MC)
    public static final int GL_RGBA8                = 0x8058;
    public static final int GL_RGBA16F              = 0x881A;
    public static final int GL_RGBA32F              = 0x8814;
    public static final int GL_R8                   = 0x8229;
    public static final int GL_RG8                  = 0x822B;

    // Texture array
    public static final int GL_TEXTURE_2D_ARRAY     = 0x8C1A;

    // Misc
    public static final int GL_HALF_FLOAT           = 0x140B;

    public static int  glGenVertexArrays()                   { return GLBackendHolder.current().genVertexArray(); }
    public static void glDeleteVertexArrays(int id)          { GLBackendHolder.current().deleteVertexArray(id); }
    public static void glBindVertexArray(int id)             { GLBackendHolder.current().bindVertexArray(id); }

    public static int  glGenFramebuffers()                   { return GLBackendHolder.current().genFramebuffer(); }
    public static void glDeleteFramebuffers(int id)          { GLBackendHolder.current().deleteFramebuffer(id); }
    public static void glBindFramebuffer(int t, int id)      { GLBackendHolder.current().bindFramebuffer(t, id); }
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLBackendHolder.current().framebufferTexture2D(target, attachment, textarget, texture, level);
    }
    public static int  glCheckFramebufferStatus(int target)  { return GLBackendHolder.current().checkFramebufferStatus(target); }
    public static void glDrawBuffers(IntBuffer bufs)         { GLBackendHolder.current().drawBuffers(bufs); }

    public static int  glGenRenderbuffers()                  { return GLBackendHolder.current().genRenderbuffer(); }
    public static void glDeleteRenderbuffers(int id)         { GLBackendHolder.current().deleteRenderbuffer(id); }
    public static void glBindRenderbuffer(int t, int id)     { GLBackendHolder.current().bindRenderbuffer(t, id); }
    public static void glRenderbufferStorage(int t, int internalformat, int w, int h) {
        GLBackendHolder.current().renderbufferStorage(t, internalformat, w, h);
    }
    public static void glFramebufferRenderbuffer(int target, int attachment, int rbtarget, int rb) {
        GLBackendHolder.current().framebufferRenderbuffer(target, attachment, rbtarget, rb);
    }
    public static void glGenerateMipmap(int target)          { GLBackendHolder.current().generateMipmap(target); }

    public static void glVertexAttribIPointer(int i, int size, int type, int stride, long offset) {
        GLBackendHolder.current().vertexAttribIPointer(i, size, type, stride, offset);
    }

    // Map buffer range — WebGL2 has no equivalent; return null/dummy.
    public static java.nio.ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
        return java.nio.ByteBuffer.allocateDirect(length).order(java.nio.ByteOrder.nativeOrder());
    }
    public static java.nio.ByteBuffer glMapBufferRange(int target, int offset, int length, int access, java.nio.ByteBuffer recycle) {
        return java.nio.ByteBuffer.allocateDirect(length).order(java.nio.ByteOrder.nativeOrder());
    }
    public static void glFlushMappedBufferRange(int target, int offset, int length) { /* no-op */ }

    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                                         int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) { /* no-op */ }

    private GL30() {}
}
