package java.util.concurrent.atomic;

public class AtomicIntegerArray {
    private final int[] arr;
    public AtomicIntegerArray(int length) { this.arr = new int[length]; }
    public AtomicIntegerArray(int[] array) { this.arr = array.clone(); }
    public final int length() { return arr.length; }
    public final int get(int i) { return arr[i]; }
    public final void set(int i, int newValue) { arr[i] = newValue; }
    public final void lazySet(int i, int newValue) { arr[i] = newValue; }
    public final int getAndSet(int i, int newValue) { int old = arr[i]; arr[i] = newValue; return old; }
    public final boolean compareAndSet(int i, int expect, int update) { if (arr[i] == expect) { arr[i] = update; return true; } return false; }
    public final boolean weakCompareAndSet(int i, int expect, int update) { return compareAndSet(i, expect, update); }
    public final int getAndIncrement(int i) { return arr[i]++; }
    public final int getAndDecrement(int i) { return arr[i]--; }
    public final int getAndAdd(int i, int delta) { int old = arr[i]; arr[i] += delta; return old; }
    public final int incrementAndGet(int i) { return ++arr[i]; }
    public final int decrementAndGet(int i) { return --arr[i]; }
    public final int addAndGet(int i, int delta) { arr[i] += delta; return arr[i]; }
    @Override public String toString() { return java.util.Arrays.toString(arr); }
}
