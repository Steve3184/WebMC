package java.util.concurrent.atomic;

public class AtomicLongArray {
    private final long[] arr;
    public AtomicLongArray(int length) { this.arr = new long[length]; }
    public AtomicLongArray(long[] array) { this.arr = array.clone(); }
    public final int length() { return arr.length; }
    public final long get(int i) { return arr[i]; }
    public final void set(int i, long newValue) { arr[i] = newValue; }
    public final void lazySet(int i, long newValue) { arr[i] = newValue; }
    public final long getAndSet(int i, long newValue) { long old = arr[i]; arr[i] = newValue; return old; }
    public final boolean compareAndSet(int i, long expect, long update) { if (arr[i] == expect) { arr[i] = update; return true; } return false; }
    public final boolean weakCompareAndSet(int i, long expect, long update) { return compareAndSet(i, expect, update); }
    public final long getAndIncrement(int i) { return arr[i]++; }
    public final long getAndDecrement(int i) { return arr[i]--; }
    public final long getAndAdd(int i, long delta) { long old = arr[i]; arr[i] += delta; return old; }
    public final long incrementAndGet(int i) { return ++arr[i]; }
    public final long decrementAndGet(int i) { return --arr[i]; }
    public final long addAndGet(int i, long delta) { arr[i] += delta; return arr[i]; }
    @Override public String toString() { return java.util.Arrays.toString(arr); }
}
