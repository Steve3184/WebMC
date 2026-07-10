package java.util.concurrent.atomic;

public class AtomicMarkableReference<V> {
    private final java.util.concurrent.atomic.AtomicReference<Pair<V>> ref;
    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        ref = new java.util.concurrent.atomic.AtomicReference<>(new Pair<>(initialRef, initialMark));
    }
    public V get(boolean[] markHolder) {
        Pair<V> p = ref.get();
        markHolder[0] = p.mark;
        return p.ref;
    }
    public void set(V newRef, boolean newMark) { ref.set(new Pair<>(newRef, newMark)); }
    public boolean attemptMark(V expectedRef, boolean newMark) {
        Pair<V> current = ref.get();
        return current.ref == expectedRef && ref.compareAndSet(current, new Pair<>(expectedRef, newMark));
    }
    private static class Pair<V> { final V ref; final boolean mark; Pair(V r, boolean m) { ref = r; mark = m; } }
}
