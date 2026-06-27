package org.lwjgl.system;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Stub of {@code org.lwjgl.system.MemoryUtil}. In native LWJGL this is a thin
 * wrapper over malloc/free producing pointer-addressed direct buffers. Here
 * we use {@code ByteBuffer.allocateDirect} (TeaVM maps direct ByteBuffer to
 * ArrayBuffer); 'pointers' are integer handles into a tracking table.
 *
 * Caveats:
 *  - MC code that does pointer arithmetic on the {@code long} returned by
 *    {@link #memAddress(ByteBuffer)} will not work. The patch layer must rewrite
 *    such call sites.
 *  - {@code memAlloc/memFree} are tracked so we can detect leaks in dev mode.
 */
public final class MemoryUtil {

    public static final long NULL = 0L;

    /**
     * Native LWJGL exposes {@code MemoryAllocator} as a pluggable allocator
     * (jemalloc / system / debug). Our backing is always direct ByteBuffer;
     * the alloc / realloc / free methods route to the shared GC heap.
     */
    public interface MemoryAllocator {
        long malloc(long size);
        long calloc(long num, long size);
        long realloc(long ptr, long size);
        void free(long ptr);
        long getAllocatedSize();
    }

    private static final java.util.concurrent.atomic.AtomicLong POINTER_SEQ = new java.util.concurrent.atomic.AtomicLong(0x1000_0000L);
    private static final java.util.concurrent.ConcurrentSkipListMap<Long, ByteBuffer> POINTER_MAP = new java.util.concurrent.ConcurrentSkipListMap<>();

    private static long lastBase;
    private static ByteBuffer lastBuf;
    private static int lastCap;

    private static long registerBuffer(ByteBuffer bb) {
        long ptr = POINTER_SEQ.getAndAdd(Math.max(bb.capacity(), 16) + 16);
        POINTER_MAP.put(ptr, bb);
        return ptr;
    }

    private static final MemoryAllocator STUB_ALLOCATOR = new MemoryAllocator() {
        @Override public long malloc(long size) {
            if (size < 0L) return 0L;
            // ByteBuffer.allocateDirect(0) is legal; still register so the pointer is non-zero.
            ByteBuffer bb = ByteBuffer.allocateDirect((int) Math.max(size, 1L)).order(ByteOrder.nativeOrder());
            return registerBuffer(bb);
        }
        @Override public long calloc(long num, long size) {
            return malloc(num * size);
        }
        @Override public long realloc(long ptr, long size) {
            if (ptr == 0L) return malloc(size);
            if (size < 0L) { free(ptr); return 0L; }
            ByteBuffer old = POINTER_MAP.remove(ptr);
            ByteBuffer next = ByteBuffer.allocateDirect((int) Math.max(size, 1L)).order(ByteOrder.nativeOrder());
            if (old != null) {
                ByteBuffer o = old.duplicate();
                o.position(0).limit(Math.min(o.capacity(), (int) Math.max(size, 1L)));
                next.put(o);
                next.position(0);
            }
            return registerBuffer(next);
        }
        @Override public void free(long ptr) {
            if (ptr != 0L) POINTER_MAP.remove(ptr);
        }
        @Override public long getAllocatedSize() {
            long total = 0;
            for (ByteBuffer bb : POINTER_MAP.values()) total += bb.capacity();
            return total;
        }
    };

    public static MemoryAllocator getAllocator()                  { return STUB_ALLOCATOR; }
    public static MemoryAllocator getAllocator(boolean trackable) { return STUB_ALLOCATOR; }

    /** UTF-8 string encode/decode helpers MC uses. */
    public static ByteBuffer memUTF8(CharSequence s) {
        byte[] bytes = s.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length + 1).order(ByteOrder.nativeOrder());
        bb.put(bytes).put((byte) 0).flip();
        return bb;
    }

    public static ByteBuffer memUTF8(CharSequence s, boolean nullTerminated) {
        byte[] bytes = s.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length + (nullTerminated ? 1 : 0))
                                  .order(ByteOrder.nativeOrder());
        bb.put(bytes);
        if (nullTerminated) bb.put((byte) 0);
        bb.flip();
        return bb;
    }

    public static String memUTF8(ByteBuffer bb) {
        if (bb == null) return null;
        byte[] arr = new byte[bb.remaining()];
        bb.duplicate().get(arr);
        // strip trailing NUL
        int len = arr.length;
        while (len > 0 && arr[len - 1] == 0) len--;
        return new String(arr, 0, len, StandardCharsets.UTF_8);
    }

    public static String memASCII(ByteBuffer bb) {
        if (bb == null) return null;
        byte[] arr = new byte[bb.remaining()];
        bb.duplicate().get(arr);
        return new String(arr, StandardCharsets.US_ASCII);
    }

    /** Long-pointer overload. Native LWJGL reads NUL-terminated UTF-8 from address; we have no real pointers — return placeholder. */
    public static String memUTF8(long address)            { return ""; }
    public static String memUTF8(long address, int length){ return ""; }
    public static String memUTF8Safe(long address)        { return ""; }

    /** Reverse lookup: ByteBuffer identity → POINTER_MAP key. */
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Long> BUFFER_TO_PTR = new java.util.concurrent.ConcurrentHashMap<>();

    public static ByteBuffer memAlloc(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(Math.max(size, 1)).order(ByteOrder.nativeOrder());
        long ptr = registerBuffer(bb);
        BUFFER_TO_PTR.put(System.identityHashCode(bb), ptr);
        return bb;
    }
    public static IntBuffer  memAllocInt(int count)          { return memAlloc(count * 4).asIntBuffer(); }
    public static FloatBuffer memAllocFloat(int count)       { return memAlloc(count * 4).asFloatBuffer(); }
    public static ShortBuffer memAllocShort(int count)       { return memAlloc(count * 2).asShortBuffer(); }
    public static void memFree(java.nio.Buffer buffer) {
        if (buffer == null) return;
        int id = System.identityHashCode(buffer);
        Long ptr = BUFFER_TO_PTR.remove(id);
        if (ptr != null) POINTER_MAP.remove(ptr);
    }

    public static ByteBuffer memCalloc(int size)             { return memAlloc(size); /* allocateDirect zeroes */ }
    public static IntBuffer  memCallocInt(int count)         { return memAllocInt(count); }
    public static ByteBuffer memRealloc(ByteBuffer src, int newSize) {
        ByteBuffer dst = memAlloc(newSize);
        if (src != null) {
            // Preserve src.position so callers reading into the buffer
            // (TextureUtil.readResource → channel.read loop) keep writing
            // at the next offset instead of clobbering prior reads.
            int srcPos = src.position();
            ByteBuffer s = src.duplicate();
            s.position(0).limit(Math.min(s.capacity(), newSize));
            dst.put(s);
            memFree(src);
            dst.position(Math.min(srcPos, newSize));
        } else {
            dst.position(0);
        }
        return dst;
    }
    public static IntBuffer memRealloc(IntBuffer src, int newCount) {
        return memRealloc((ByteBuffer)null, newCount * 4).asIntBuffer();
    }
    public static FloatBuffer memRealloc(FloatBuffer src, int newCount) {
        return memRealloc((ByteBuffer)null, newCount * 4).asFloatBuffer();
    }

    /**
     * In native LWJGL: address of the buffer in process memory. We return
     * the POINTER_MAP key registered at allocation time. If the buffer
     * wasn't allocated by us, we register it on-demand so subsequent
     * memGetInt/memPutInt calls work.
     */
    public static long memAddress(ByteBuffer bb) {
        if (bb == null) return 0L;
        int id = System.identityHashCode(bb);
        Long ptr = BUFFER_TO_PTR.get(id);
        if (ptr != null) return ptr;
        // On-demand registration for foreign ByteBuffers
        long newPtr = registerBuffer(bb);
        BUFFER_TO_PTR.put(id, newPtr);
        return newPtr;
    }
    public static long memAddress(IntBuffer bb) {
        if (bb == null) return 0L;
        // IntBuffer doesn't have a direct identity; return a hash-based handle
        return System.identityHashCode(bb) & 0xFFFFFFFFL;
    }
    public static long memAddress(FloatBuffer bb) {
        if (bb == null) return 0L;
        return System.identityHashCode(bb) & 0xFFFFFFFFL;
    }
    public static long memAddress0(ByteBuffer bb) { return memAddress(bb); }

    /** Untracked native alloc/free. We have no malloc; route through STUB_ALLOCATOR so callers get a usable handle. */
    public static long nmemAlloc(long size)            { return STUB_ALLOCATOR.malloc(size); }
    public static long nmemCalloc(long num, long size) { return STUB_ALLOCATOR.calloc(num, size); }
    public static long nmemRealloc(long ptr, long size){ return STUB_ALLOCATOR.realloc(ptr, size); }
    public static void nmemFree(long ptr)              { STUB_ALLOCATOR.free(ptr); }

    public static int  memGetInt(long address) {
        ByteBuffer bb = resolvePointer(address);
        if (bb == null) return 0;
        return bb.getInt(0);
    }
    public static byte memGetByte(long address) {
        ByteBuffer bb = resolvePointer(address);
        if (bb == null) return 0;
        return bb.get(0);
    }
    public static short memGetShort(long address) {
        ByteBuffer bb = resolvePointer(address);
        if (bb == null) return 0;
        return bb.getShort(0);
    }
    public static long memGetLong(long address) {
        ByteBuffer bb = resolvePointer(address);
        if (bb == null) return 0L;
        return bb.getLong(0);
    }
    public static float memGetFloat(long address)            { return 0f; }
    public static double memGetDouble(long address)          { return 0.0; }
    public static void memPutInt(long address, int v) {
        ByteBuffer bb = resolvePointer(address);
        if (bb != null) bb.putInt(0, v);
    }
    public static void memPutByte(long address, byte v) {
        ByteBuffer bb = resolvePointer(address);
        if (bb != null) bb.put(0, v);
    }
    public static void memPutShort(long address, short v) {
        ByteBuffer bb = resolvePointer(address);
        if (bb != null) bb.putShort(0, v);
    }
    public static void memPutLong(long address, long v) {
        ByteBuffer bb = resolvePointer(address);
        if (bb != null) bb.putLong(0, v);
    }
    public static void memPutFloat(long address, float v)    { /* no-op */ }
    public static void memPutDouble(long address, double v)  { /* no-op */ }

    /**
     * Resolve a fake pointer (possibly with offset) back to a ByteBuffer
     * view positioned at the corresponding byte offset. Hot path for
     * MipmapGenerator / NativeImage: 6M calls per atlas reload.
     *
     * Implementation:
     *   1. last-hit cache (base, buf, cap) — covers getPixel/setPixel chains
     *      hitting the same allocation hundreds of thousands of times.
     *   2. O(1) exact-key lookup for address == base.
     *   3. O(log N) floorEntry for address == base + offset.
     */
    private static ByteBuffer resolvePointer(long address) {
        if (address == 0L) return null;

        long lb = lastBase;
        ByteBuffer lbf = lastBuf;
        int lcap = lastCap;
        if (lbf != null && address >= lb && address - lb < lcap) {
            ByteBuffer v = lbf.duplicate().order(lbf.order());
            v.position((int)(address - lb));
            return v;
        }

        ByteBuffer bb = POINTER_MAP.get(address);
        if (bb != null) {
            lastBase = address; lastBuf = bb; lastCap = bb.capacity();
            ByteBuffer v = bb.duplicate().order(bb.order());
            v.position(0);
            return v;
        }

        java.util.Map.Entry<Long, ByteBuffer> floor = POINTER_MAP.floorEntry(address);
        if (floor == null) return null;
        long base = floor.getKey();
        ByteBuffer buf = floor.getValue();
        if (buf == null) return null;
        int offset = (int)(address - base);
        if (offset < 0 || offset >= buf.capacity()) return null;
        lastBase = base; lastBuf = buf; lastCap = buf.capacity();
        ByteBuffer v = buf.duplicate().order(buf.order());
        v.position(offset);
        return v;
    }

    /** Wrap a pointer-region as a ByteBuffer. We can't actually wrap raw memory — return a fresh allocation as placeholder. */
    public static ByteBuffer memByteBuffer(long address, int capacity) {
        ByteBuffer registered = lookupBuffer(address, capacity);
        if (registered != null) return registered;
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }
    public static IntBuffer  memIntBuffer(long address, int capacity)  { return memByteBuffer(address, capacity * 4).asIntBuffer(); }
    public static FloatBuffer memFloatBuffer(long address, int capacity){ return memByteBuffer(address, capacity * 4).asFloatBuffer(); }

    /**
     * Resolve a fake pointer back to its registered ByteBuffer, returning
     * a slice covering [address, address+capacity). O(log N) via floorEntry.
     */
    private static ByteBuffer lookupBuffer(long address, int capacity) {
        if (address == 0L || capacity <= 0) return null;
        ByteBuffer bb = POINTER_MAP.get(address);
        long base = address;
        if (bb == null) {
            java.util.Map.Entry<Long, ByteBuffer> floor = POINTER_MAP.floorEntry(address);
            if (floor == null) return null;
            ByteBuffer cand = floor.getValue();
            if (cand == null) return null;
            long kv = floor.getKey();
            if (address - kv + capacity > cand.capacity()) return null;
            bb = cand;
            base = kv;
        }
        int offset = (int) (address - base);
        if (offset < 0 || offset + capacity > bb.capacity()) return null;
        ByteBuffer view = bb.duplicate().order(bb.order());
        view.position(offset).limit(offset + capacity);
        return view.slice().order(bb.order());
    }

    public static ByteBuffer memCopy(ByteBuffer src, ByteBuffer dst) {
        ByteBuffer s = src.duplicate();
        dst.put(s);
        return dst;
    }
    /** Long-address copy overloads. Uses resolvePointer to find real buffers. */
    public static void memCopy(long src, long dst, int bytes) { memCopy(src, dst, (long)bytes); }
    public static void memCopy(long src, long dst, long bytes) {
        if (bytes <= 0 || src == 0L || dst == 0L) return;
        ByteBuffer srcBuf = resolvePointer(src);
        ByteBuffer dstBuf = resolvePointer(dst);
        if (srcBuf == null || dstBuf == null) return;
        // Find exact offsets
        int count = (int) Math.min(bytes, Math.min(srcBuf.remaining(), dstBuf.remaining()));
        for (int i = 0; i < count; i++) {
            dstBuf.put(dstBuf.position() + i, srcBuf.get(srcBuf.position() + i));
        }
    }
    public static void memSet(long ptr, int value, int bytes) { memSet(ptr, value, (long) bytes); }
    public static void memSet(long ptr, int value, long bytes) {
        if (bytes <= 0 || ptr == 0L) return;
        ByteBuffer buf = resolvePointer(ptr);
        if (buf == null) return;
        byte val = (byte)(value & 0xFF);
        int count = (int) Math.min(bytes, buf.remaining());
        for (int i = 0; i < count; i++) {
            buf.put(buf.position() + i, val);
        }
    }

    public static ByteBuffer memSlice(ByteBuffer bb, int offset, int capacity) {
        ByteBuffer s = bb.duplicate().order(bb.order());
        s.position(offset).limit(offset + capacity);
        return s.slice().order(bb.order());
    }

    private MemoryUtil() {}
}
