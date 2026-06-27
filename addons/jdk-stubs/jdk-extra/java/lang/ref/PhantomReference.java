package java.lang.ref;

public final class PhantomReference<T> extends Reference<T> {
    public PhantomReference(T referent, ReferenceQueue<? super T> q) { super(referent, q); }
    @Override public T get() { return null; }
}
