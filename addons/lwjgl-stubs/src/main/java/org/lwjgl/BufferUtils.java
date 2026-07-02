package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Stub of {@code org.lwjgl.BufferUtils}. Pure Java; works under TeaVM as long
 * as direct ByteBuffer is mapped to ArrayBuffer (the default).
 */
public final class BufferUtils {
    public static ByteBuffer  createByteBuffer(int size)   { return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()); }
    public static IntBuffer   createIntBuffer(int count)   { return createByteBuffer(count * 4).asIntBuffer(); }
    public static FloatBuffer createFloatBuffer(int count) { return createByteBuffer(count * 4).asFloatBuffer(); }
    public static ShortBuffer createShortBuffer(int count) { return createByteBuffer(count * 2).asShortBuffer(); }
    private BufferUtils() {}
}
