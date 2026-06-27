package java.util.concurrent;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class ConcurrentLinkedDeque<E> extends java.util.AbstractCollection<E> implements Deque<E> {
    private final LinkedList<E> backing = new LinkedList<>();
    public ConcurrentLinkedDeque() {}
    public ConcurrentLinkedDeque(Collection<? extends E> c) { backing.addAll(c); }

    @Override public Iterator<E> iterator() { return backing.iterator(); }
    @Override public int size() { return backing.size(); }
    @Override public boolean isEmpty() { return backing.isEmpty(); }

    @Override public void addFirst(E e) { backing.addFirst(e); }
    @Override public void addLast(E e) { backing.addLast(e); }
    @Override public boolean offerFirst(E e) { backing.addFirst(e); return true; }
    @Override public boolean offerLast(E e) { backing.addLast(e); return true; }
    @Override public E removeFirst() { return backing.removeFirst(); }
    @Override public E removeLast() { return backing.removeLast(); }
    @Override public E pollFirst() { return backing.pollFirst(); }
    @Override public E pollLast() { return backing.pollLast(); }
    @Override public E getFirst() { return backing.getFirst(); }
    @Override public E getLast() { return backing.getLast(); }
    @Override public E peekFirst() { return backing.peekFirst(); }
    @Override public E peekLast() { return backing.peekLast(); }
    @Override public boolean removeFirstOccurrence(Object o) { return backing.removeFirstOccurrence(o); }
    @Override public boolean removeLastOccurrence(Object o) { return backing.removeLastOccurrence(o); }
    @Override public boolean offer(E e) { return backing.offer(e); }
    @Override public E remove() { return backing.remove(); }
    @Override public E poll() { return backing.poll(); }
    @Override public E element() { return backing.element(); }
    @Override public E peek() { return backing.peek(); }
    @Override public void push(E e) { backing.push(e); }
    @Override public E pop() { return backing.pop(); }
    @Override public Iterator<E> descendingIterator() { return backing.descendingIterator(); }
    @Override public boolean add(E e) { return backing.add(e); }
    @Override public boolean remove(Object o) { return backing.remove(o); }
}
