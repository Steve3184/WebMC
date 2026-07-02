package org.lwjgl.opengl;

/** Stub of ARBBufferStorage extension. WebGL2 has no equivalent — buffer must use bufferData. */
public final class ARBBufferStorage {
    public static final int GL_MAP_PERSISTENT_BIT = 0x40;
    public static final int GL_MAP_COHERENT_BIT   = 0x80;
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x100;
    public static final int GL_CLIENT_STORAGE_BIT  = 0x200;
    public static final int GL_MAP_READ_BIT  = 0x1;
    public static final int GL_MAP_WRITE_BIT = 0x2;

    public static void glBufferStorage(int target, java.nio.ByteBuffer data, int flags) {}
    public static void glBufferStorage(int target, long size, int flags) {}
    public static void nglBufferStorage(int target, long size, long data, int flags) {}

    private ARBBufferStorage() {}
}
