package org.lwjgl.opengl;

/** Stub of ARBVertexAttribBinding (uniform-style vertex layout). WebGL2 has no equivalent — emulate. */
public final class ARBVertexAttribBinding {
    public static void glBindVertexBuffer(int index, int buf, long offset, int stride) {}
    public static void glVertexAttribFormat(int index, int size, int type, boolean normalized, int relOffset) {}
    public static void glVertexAttribIFormat(int index, int size, int type, int relOffset) {}
    public static void glVertexAttribBinding(int attribIdx, int bindIdx) {}
    public static void glVertexBindingDivisor(int bindIdx, int divisor) {}
    private ARBVertexAttribBinding() {}
}
