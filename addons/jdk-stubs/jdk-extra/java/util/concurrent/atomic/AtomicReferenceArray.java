package java.util.concurrent.atomic;

public class AtomicReferenceArray<E> {
    private final Object[] arr;
    public AtomicReferenceArray(int length) { this.arr = new Object[length]; }
    public AtomicReferenceArray(E[] array) { this.arr = array.clone(); }
    public final int length() { return arr.length; }
    @SuppressWarnings("unchecked") public final E get(int i) { return (E) arr[i]; }
    public final void set(int i, E newValue) { arr[i] = newValue; }
    public final void lazySet(int i, E newValue) { arr[i] = newValue; }
    @SuppressWarnings("unchecked") public final E getAndSet(int i, E newValue) { Object old = arr[i]; arr[i] = newValue; return (E) old; }
    public final boolean compareAndSet(int i, E expect, E update) { if (arr[i] == expect) { arr[i] = update; return true; } return false; }
    public final boolean weakCompareAndSet(int i, E expect, E update) { return compareAndSet(i, expect, update); }
    @SuppressWarnings("unchecked") public final E compareAndExchange(int i, E expect, E update) {
        E old = (E) arr[i];
        if (old == expect) arr[i] = update;
        return old;
    }
    @SuppressWarnings("unchecked") public final E getAndUpdate(int i, java.util.function.UnaryOperator<E> updater) {
        E old = (E) arr[i]; arr[i] = updater.apply(old); return old;
    }
    @SuppressWarnings("unchecked") public final E updateAndGet(int i, java.util.function.UnaryOperator<E> updater) {
        E v = updater.apply((E) arr[i]); arr[i] = v; return v;
    }
    @SuppressWarnings("unchecked") public final E getAndAccumulate(int i, E x, java.util.function.BinaryOperator<E> acc) {
        E old = (E) arr[i]; arr[i] = acc.apply(old, x); return old;
    }
    @SuppressWarnings("unchecked") public final E accumulateAndGet(int i, E x, java.util.function.BinaryOperator<E> acc) {
        E v = acc.apply((E) arr[i], x); arr[i] = v; return v;
    }
    @Override public String toString() { return java.util.Arrays.toString(arr); }
}
