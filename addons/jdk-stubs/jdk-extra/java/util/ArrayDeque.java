package java.util;

import java.io.Serializable;

public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable {
    private Object[] elements;
    private int head;
    private int tail;
    private static final int MIN_INITIAL_CAPACITY = 8;

    public ArrayDeque() {
        elements = new Object[MIN_INITIAL_CAPACITY];
    }

    public ArrayDeque(int numElements) {
        elements = new Object[calculateSize(numElements)];
    }

    public ArrayDeque(Collection<? extends E> c) {
        this(c.size());
        for (E e : c) {
            add(e);
        }
    }

    private static int calculateSize(int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        while (initialCapacity < numElements) {
            initialCapacity <<= 1;
        }
        return initialCapacity;
    }

    @Override
    public void addFirst(E e) {
        if (e == null) throw new NullPointerException();
        elements[tail = (tail - 1) & (elements.length - 1)] = e;
    }

    @Override
    public void addLast(E e) {
        if (e == null) throw new NullPointerException();
        elements[tail] = e;
        if ((tail = (tail + 1) & (elements.length - 1)) == head) doubleCapacity();
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E removeFirst() {
        E x = pollFirst();
        if (x == null) throw new NoSuchElementException();
        return x;
    }

    @Override
    public E removeLast() {
        E x = pollLast();
        if (x == null) throw new NoSuchElementException();
        return x;
    }

    @Override
    public E pollFirst() {
        int h = head;
        E result = (E) elements[h];
        if (result == null) return null;
        elements[h] = null;
        head = (h + 1) & (elements.length - 1);
        return result;
    }

    @Override
    public E pollLast() {
        int t = (tail - 1) & (elements.length - 1);
        E result = (E) elements[t];
        if (result == null) return null;
        elements[t] = null;
        tail = t;
        return result;
    }

    @Override
    public E getFirst() {
        E x = (E) elements[head];
        if (x == null) throw new NoSuchElementException();
        return x;
    }

    @Override
    public E getLast() {
        E x = (E) elements[(tail - 1) & (elements.length - 1)];
        if (x == null) throw new NoSuchElementException();
        return x;
    }

    @Override
    public E peekFirst() {
        return (E) elements[head];
    }

    @Override
    public E peekLast() {
        return (E) elements[(tail - 1) & (elements.length - 1)];
    }

    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public boolean offer(E e) {
        addLast(e);
        return true;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) return false;
        int mask = elements.length - 1;
        int i = head;
        while (i != tail) {
            if (o.equals(elements[i])) {
                int prev = (i - 1) & mask;
                int next = (i + 1) & mask;
                if (i < head) {
                    if (prev == tail) head = next;
                } else if (i == tail) {
                    tail = prev;
                } else {
                    int hd = (head - i) & mask;
                    int tl = (tail - i) & mask;
                    if (hd < tl) {
                        head = (head - 1) & mask;
                        elements[i] = elements[prev];
                        i = prev;
                    } else {
                        tail = (tail - 1) & mask;
                        elements[i] = elements[next];
                        i = next;
                    }
                }
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return removeFirstOccurrence(o);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public boolean isEmpty() {
        return head == tail;
    }

    @Override
    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    @Override
    public void clear() {
        int h = head;
        int t = tail;
        if (h != t) {
            head = tail = 0;
            int i = h;
            int mask = elements.length - 1;
            do {
                elements[i] = null;
                i = (i + 1) & mask;
            } while (i != t);
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        int mask = elements.length - 1;
        int i = head;
        Object x;
        while ((x = elements[i]) != null) {
            if (o.equals(x)) return true;
            i = (i + 1) & mask;
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        int i = head;
        int mask = elements.length - 1;
        int j = 0;
        Object x;
        while ((x = elements[i]) != null) {
            a[j++] = (T) x;
            i = (i + 1) & mask;
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    private void doubleCapacity() {
        int p = head;
        int n = elements.length;
        int r = n - p;
        int newCapacity = n << 1;
        if (newCapacity < 0) throw new IllegalStateException("Deque too big");
        Object[] a = new Object[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }

    @Override
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {
        private int cursor = head;
        private int fence = tail;
        private int lastRet = -1;

        @Override
        public boolean hasNext() {
            return cursor != fence;
        }

        @Override
        public E next() {
            if (cursor == fence) throw new NoSuchElementException();
            E result = (E) elements[cursor];
            lastRet = cursor;
            cursor = (cursor + 1) & (elements.length - 1);
            return result;
        }

        @Override
        public void remove() {
            if (lastRet == -1) throw new IllegalStateException();
            int prev = (lastRet - 1) & (elements.length - 1);
            int next = (lastRet + 1) & (elements.length - 1);
            if (lastRet < cursor) {
                elements[lastRet] = elements[prev];
                head = (head - 1) & (elements.length - 1);
            } else {
                elements[lastRet] = elements[next];
                tail = (tail - 1) & (elements.length - 1);
            }
            lastRet = -1;
        }
    }

    private class DescendingIterator implements Iterator<E> {
        private int cursor = tail;
        private int lastRet = -1;

        @Override
        public boolean hasNext() {
            return cursor != head;
        }

        @Override
        public E next() {
            cursor = (cursor - 1) & (elements.length - 1);
            E result = (E) elements[cursor];
            lastRet = cursor;
            return result;
        }

        @Override
        public void remove() {
            if (lastRet == -1) throw new IllegalStateException();
            // Descending iterator removal is not directly supported
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ArrayDeque<E> clone() {
        try {
            ArrayDeque<E> result = (ArrayDeque<E>) super.clone();
            result.elements = elements.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
