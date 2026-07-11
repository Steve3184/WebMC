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
    public V getReference() { return ref.get().ref; }
    public boolean isMarked() { return ref.get().mark; }
    public boolean weakCompareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }
    public boolean compareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        Pair<V> current = ref.get();
        return expectedReference == current.ref && expectedMark == current.mark &&
               (newReference == current.ref && newMark == current.mark ||
                ref.compareAndSet(current, new Pair<>(newReference, newMark)));
    }
    private static class Pair<V> { final V ref; final boolean mark; Pair(V r, boolean m) { ref = r; mark = m; } }
}
