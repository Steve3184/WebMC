package org.lwjgl.opengl;

import top.steve3184.webmc.teavm.gl.GLBackendHolder;
import java.nio.ByteBuffer;

/** Stub of {@code org.lwjgl.opengl.GL15}. Buffer objects + queries. */
public final class GL15 {
    public static final int GL_ARRAY_BUFFER         = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;

    public static final int GL_STREAM_DRAW  = 0x88E0;
    public static final int GL_STREAM_READ  = 0x88E1;
    public static final int GL_STREAM_COPY  = 0x88E2;
    public static final int GL_STATIC_DRAW  = 0x88E4;
    public static final int GL_STATIC_READ  = 0x88E5;
    public static final int GL_STATIC_COPY  = 0x88E6;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_DYNAMIC_READ = 0x88E9;
    public static final int GL_DYNAMIC_COPY = 0x88EA;

    public static int  glGenBuffers()                       { return GLBackendHolder.current().genBuffer(); }
    public static void glDeleteBuffers(int id)              { GLBackendHolder.current().deleteBuffer(id); }
    public static void glBindBuffer(int target, int id)     { GLBackendHolder.current().bindBuffer(target, id); }
    public static void glBufferData(int target, ByteBuffer data, int usage) { GLBackendHolder.current().bufferData(target, data, usage); }
    public static void glBufferData(int target, long size, int usage)       { /* alloc-only variant; no-op */ }
    public static void glBufferData(int target, java.nio.IntBuffer data, int usage) { /* no-op */ }
    public static void glBufferData(int target, java.nio.FloatBuffer data, int usage) { /* no-op */ }
    public static void glBufferSubData(int target, long offset, ByteBuffer data) { GLBackendHolder.current().bufferSubData(target, offset, data); }
    public static void glBufferSubData(int target, long offset, long size, long ptr) { /* no-op */ }
    public static boolean glUnmapBuffer(int target)          { return true; }

    private GL15() {}
}
