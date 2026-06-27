package java.util.concurrent;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;

/** Single-threaded JS no-op CopyOnWriteArraySet backed by HashSet. */
public class CopyOnWriteArraySet<E> extends AbstractSet<E> {
    private final HashSet<E> backing = new HashSet<>();
    public CopyOnWriteArraySet() {}
    public CopyOnWriteArraySet(Collection<? extends E> c) { backing.addAll(c); }

    @Override public Iterator<E> iterator() { return backing.iterator(); }
    @Override public int size() { return backing.size(); }
    @Override public boolean isEmpty() { return backing.isEmpty(); }
    @Override public boolean contains(Object o) { return backing.contains(o); }
    @Override public boolean add(E e) { return backing.add(e); }
    @Override public boolean remove(Object o) { return backing.remove(o); }
    @Override public boolean addAll(Collection<? extends E> c) { return backing.addAll(c); }
    @Override public void clear() { backing.clear(); }
}
