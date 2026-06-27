package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> {
    private final LinkedList<E> backing = new LinkedList<>();
    public ConcurrentLinkedQueue() {}
    public ConcurrentLinkedQueue(Collection<? extends E> c) { backing.addAll(c); }
    @Override public Iterator<E> iterator() { return backing.iterator(); }
    @Override public int size() { return backing.size(); }
    @Override public boolean offer(E e) { backing.add(e); return true; }
    @Override public E poll() { return backing.pollFirst(); }
    @Override public E peek() { return backing.peekFirst(); }
    @Override public boolean isEmpty() { return backing.isEmpty(); }
    @Override public boolean add(E e) { backing.add(e); return true; }
    @Override public boolean remove(Object o) { return backing.remove(o); }
}
