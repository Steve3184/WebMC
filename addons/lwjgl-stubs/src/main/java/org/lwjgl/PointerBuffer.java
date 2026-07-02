package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

/**
 * Stub of {@code org.lwjgl.PointerBuffer}. Native LWJGL stores raw 64-bit
 * pointers; we back it with a LongBuffer over a direct ByteBuffer. Address
 * arithmetic done by callers will not work — patch such call sites.
 */
public class PointerBuffer {
    private final LongBuffer storage;

    private PointerBuffer(LongBuffer storage) { this.storage = storage; }

    public static PointerBuffer allocateDirect(int capacity) {
        ByteBuffer bb = ByteBuffer.allocateDirect(capacity * 8).order(ByteOrder.nativeOrder());
        return new PointerBuffer(bb.asLongBuffer());
    }

    public int capacity()             { return storage.capacity(); }
    public int position()             { return storage.position(); }
    public PointerBuffer position(int p) { storage.position(p); return this; }
    public int limit()                { return storage.limit(); }
    public PointerBuffer limit(int l) { storage.limit(l); return this; }
    public PointerBuffer flip()       { storage.flip(); return this; }
    public PointerBuffer clear()      { storage.clear(); return this; }
    public int remaining()            { return storage.remaining(); }
    public boolean hasRemaining()     { return storage.hasRemaining(); }

    public long get()                 { return storage.get(); }
    public long get(int idx)          { return storage.get(idx); }
    public PointerBuffer put(long v)  { storage.put(v); return this; }
    public PointerBuffer put(int idx, long v) { storage.put(idx, v); return this; }
    public PointerBuffer put(ByteBuffer src) { /* native LWJGL stores raw pointer; no-op for stub */ return this; }
    public PointerBuffer put(java.nio.LongBuffer src) { storage.put(src); return this; }

    /** Address of the buffer's first element. We have no real address; return identity hash. */
    public long address()             { return System.identityHashCode(this) & 0xFFFFFFFFL; }
    public long address0()            { return address(); }
    public PointerBuffer slice()      { return this; /* shallow */ }
}
