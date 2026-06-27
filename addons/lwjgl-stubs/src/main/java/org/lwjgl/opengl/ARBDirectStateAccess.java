package org.lwjgl.opengl;

/** Stub of ARBDirectStateAccess (DSA). WebGL2 has no DSA — must bind first. */
public final class ARBDirectStateAccess {
    public static int  glCreateBuffers()                          { return GL15.glGenBuffers(); }
    public static int  glCreateTextures(int target)               { return GL11.glGenTextures(); }
    public static int  glCreateFramebuffers()                     { return GL30.glGenFramebuffers(); }
    public static int  glCreateRenderbuffers()                    { return GL30.glGenRenderbuffers(); }
    public static int  glCreateVertexArrays()                     { return GL30.glGenVertexArrays(); }
    public static void glNamedBufferData(int buf, java.nio.ByteBuffer data, int usage) { /* TODO */ }
    public static void glNamedBufferData(int buf, long size, int usage) { /* TODO */ }
    public static void glNamedBufferSubData(int buf, long off, java.nio.ByteBuffer data) { /* TODO */ }
    public static void glNamedBufferStorage(int buf, java.nio.ByteBuffer data, int flags) { /* TODO */ }
    public static void glNamedBufferStorage(int buf, long size, int flags) { /* TODO */ }
    public static java.nio.ByteBuffer glMapNamedBufferRange(int buf, int offset, int length, int access) {
        return java.nio.ByteBuffer.allocateDirect(Math.max(length, 0)).order(java.nio.ByteOrder.nativeOrder());
    }
    public static java.nio.ByteBuffer glMapNamedBufferRange(int buf, long offset, long length, int access) {
        return glMapNamedBufferRange(buf, (int)offset, (int)length, access);
    }
    public static boolean glUnmapNamedBuffer(int buf)             { return true; }
    public static void glFlushMappedNamedBufferRange(int buf, int offset, int length) { /* TODO */ }
    public static void glFlushMappedNamedBufferRange(int buf, long offset, long length) { /* TODO */ }
    public static void glCopyNamedBufferSubData(int read, int write, long readOffset, long writeOffset, long size) { /* TODO */ }
    public static void glCopyNamedBufferSubData(int read, int write, int readOffset, int writeOffset, int size) { /* TODO */ }
    public static void glNamedFramebufferTexture(int fb, int attachment, int texture, int level) { /* TODO */ }
    public static void glBlitNamedFramebuffer(int rfb, int dfb,
                                              int srcX0, int srcY0, int srcX1, int srcY1,
                                              int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) { /* TODO */ }
    public static void glTextureStorage2D(int t, int levels, int internalformat, int w, int h) { /* TODO */ }
    public static void glTextureSubImage2D(int t, int level, int x, int y, int w, int h, int fmt, int type, java.nio.ByteBuffer pix) { /* TODO */ }
    public static void glTextureParameteri(int t, int p, int v)   { /* TODO */ }
    public static void glGenerateTextureMipmap(int t)             { /* TODO */ }
    public static void glBindTextureUnit(int unit, int tex)       { /* TODO */ }
    private ARBDirectStateAccess() {}
}
