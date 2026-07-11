package java.util;

public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    transient Node<E> first;
    transient Node<E> last;
    transient int size = 0;

    public LinkedList() {}

    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    private void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null) last = newNode;
        else f.prev = newNode;
        size++;
        modCount++;
    }

    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null) first = newNode;
        else l.next = newNode;
        size++;
        modCount++;
    }

    void linkBefore(E e, Node<E> succ) {
        final Node<E> pred = succ.prev;
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null) first = newNode;
        else pred.next = newNode;
        size++;
        modCount++;
    }

    private E unlinkFirst(Node<E> f) {
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null;
        first = next;
        if (next == null) last = null;
        else next.prev = null;
        size--;
        modCount++;
        return element;
    }

    private E unlinkLast(Node<E> l) {
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null;
        last = prev;
        if (prev == null) first = null;
        else prev.next = null;
        size--;
        modCount++;
        return element;
    }

    E unlink(Node<E> x) {
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;
        if (prev == null) first = next;
        else {
            prev.next = next;
            x.prev = null;
        }
        if (next == null) last = prev;
        else {
            next.prev = prev;
            x.next = null;
        }
        x.item = null;
        size--;
        modCount++;
        return element;
    }

    @Override
    public E getFirst() {
        final Node<E> f = first;
        if (f == null) throw new NoSuchElementException();
        return f.item;
    }

    @Override
    public E getLast() {
        final Node<E> l = last;
        if (l == null) throw new NoSuchElementException();
        return l.item;
    }

    @Override
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null) throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    @Override
    public E removeLast() {
        final Node<E> l = last;
        if (l == null) throw new NoSuchElementException();
        return unlinkLast(l);
    }

    @Override
    public void addFirst(E e) {
        linkFirst(e);
    }

    @Override
    public void addLast(E e) {
        linkLast(e);
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void clear() {
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }

    @Override
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    @Override
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    @Override
    public void add(int index, E element) {
        checkPositionIndex(index);
        if (index == size) linkLast(element);
        else linkBefore(element, node(index));
    }

    @Override
    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    Node<E> node(int index) {
        Node<E> x;
        if (index < (size >> 1)) {
            x = first;
            for (int i = 0; i < index; i++) x = x.next;
        } else {
            x = last;
            for (int i = size - 1; i > index; i--) x = x.prev;
        }
        return x;
    }

    @Override
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) return index;
                index++;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null) return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item)) return index;
            }
        }
        return -1;
    }

    @Override
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return removeLastOccurrenceInternal(o);
    }

    private boolean removeLastOccurrenceInternal(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new Iterator<E>() {
            private Node<E> current = last;
            private boolean canRemove = false;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public E next() {
                if (current == null) throw new java.util.NoSuchElementException();
                E result = current.item;
                current = current.prev;
                canRemove = true;
                return result;
            }

            @Override
            public void remove() {
                if (!canRemove) throw new IllegalStateException();
                unlink(lastReturned);
                canRemove = false;
            }

            private Node<E> lastReturned;
        };
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListIterator<E>() {
            private Node<E> lastReturned = null;
            private Node<E> next = (index == size) ? null : node(index);
            private int nextIndex = index;

            @Override
            public boolean hasNext() { return nextIndex < size; }

            @Override
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                lastReturned = next;
                next = next.next;
                nextIndex++;
                return lastReturned.item;
            }

            @Override
            public boolean hasPrevious() { return nextIndex > 0; }

            @Override
            public E previous() {
                if (!hasPrevious()) throw new NoSuchElementException();
                nextIndex--;
                next = (next == null) ? last : node(nextIndex);
                lastReturned = next;
                return lastReturned.item;
            }

            @Override
            public int nextIndex() { return nextIndex; }

            @Override
            public int previousIndex() { return nextIndex - 1; }

            @Override
            public void remove() {
                if (lastReturned == null) throw new IllegalStateException();
                Node<E> lastNext = lastReturned.next;
                unlink(lastReturned);
                if (lastReturned == next) {
                    next = lastNext;
                } else {
                    nextIndex--;
                }
                lastReturned = null;
            }

            @Override
            public void set(E e) {
                if (lastReturned == null) throw new IllegalStateException();
                lastReturned.item = e;
            }

            @Override
            public void add(E e) {
                if (next == null) linkLast(e);
                else linkBefore(e, next);
                lastReturned = null;
                nextIndex++;
            }
        };
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
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
    public E peekFirst() {
        return peek();
    }

    @Override
    public E peekLast() {
        final Node<E> l = last;
        return l == null ? null : l.item;
    }

    @Override
    public E pollFirst() {
        return poll();
    }

    @Override
    public E pollLast() {
        final Node<E> l = last;
        return l == null ? null : unlinkLast(l);
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public Iterator<E> iterator() {
        return new ListIterator<E>() {
            private int index = 0;
            private Node<E> lastReturned = null;
            private Node<E> next = first;
            private int expectedModCount = modCount;

            private Node<E> assertNext() {
                if (modCount != expectedModCount) throw new java.util.ConcurrentModificationException();
                if (next == null) throw new NoSuchElementException();
                return next;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public E next() {
                lastReturned = assertNext();
                E item = lastReturned.item;
                lastReturned = lastReturned.next;
                next = lastReturned;
                index++;
                return item;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public E previous() {
                return null;
            }

            @Override
            public int nextIndex() {
                return index;
            }

            @Override
            public int previousIndex() {
                return index - 1;
            }

            @Override
            public void remove() {
                if (lastReturned == null) throw new IllegalStateException();
                unlink(lastReturned);
                expectedModCount = modCount;
            }

            @Override
            public void set(E e) {
                if (lastReturned == null) throw new IllegalStateException();
                lastReturned.item = e;
            }

            @Override
            public void add(E e) {
                if (lastReturned == null) throw new IllegalStateException();
                linkBefore(e, next);
                expectedModCount = modCount;
            }
        };
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
}
