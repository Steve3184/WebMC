package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/** Single-threaded JS no-op LinkedBlockingQueue backed by LinkedList. */
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final LinkedList<E> backing = new LinkedList<>();
    private final int capacity;
    public LinkedBlockingQueue() { this.capacity = Integer.MAX_VALUE; }
    public LinkedBlockingQueue(int capacity) { this.capacity = capacity; }
    public LinkedBlockingQueue(Collection<? extends E> c) { this.capacity = Integer.MAX_VALUE; backing.addAll(c); }

    @Override public Iterator<E> iterator() { return backing.iterator(); }
    @Override public int size() { return backing.size(); }
    @Override public boolean offer(E e) { if (backing.size() >= capacity) return false; backing.add(e); return true; }
    @Override public E poll() { return backing.pollFirst(); }
    @Override public E peek() { return backing.peekFirst(); }
    @Override public void put(E e) { backing.add(e); }
    @Override public boolean offer(E e, long timeout, TimeUnit unit) { return offer(e); }
    @Override public E take() { return backing.pollFirst(); }
    @Override public E poll(long timeout, TimeUnit unit) { return backing.pollFirst(); }
    @Override public int remainingCapacity() { return capacity - backing.size(); }
    @Override public int drainTo(Collection<? super E> c) { int n = backing.size(); c.addAll(backing); backing.clear(); return n; }
    @Override public int drainTo(Collection<? super E> c, int maxElements) {
        int n = 0;
        while (!backing.isEmpty() && n < maxElements) { c.add(backing.pollFirst()); n++; }
        return n;
    }
}
