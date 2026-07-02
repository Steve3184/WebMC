package org.lwjgl.stb;

import java.nio.ByteBuffer;

/** STB image-write callback. MC subclasses this for PNG screenshots. */
public abstract class STBIWriteCallback {
    public abstract void invoke(long context, long data, int size);
    public void free()    {}
    public void close()   {}
    public long address() { return 0L; }
    public static STBIWriteCallback create(STBIWriteCallbackI l) {
        return new STBIWriteCallback() { @Override public void invoke(long c, long d, int s) { l.invoke(c, d, s); } };
    }
    /** Helper used by MC: copy callback's buffer into a Java ByteBuffer. */
    public static ByteBuffer getData(long data, int size) { return ByteBuffer.allocate(size); }
}
