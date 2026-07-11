package java.util;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected int modCount = 0;

    protected AbstractList() {}

    @Override
    public abstract E get(int index);

    @Override
    public Iterator<E> iterator() {
        return new ListItr(0);
    }

    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    public ListIterator<E> listIterator(int index) {
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        int cursor;
        int lastRet = -1;
        int expectedModCount = modCount;

        ListItr(int index) { cursor = index; }

        @Override public boolean hasNext() { return cursor != size(); }
        @Override public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size()) throw new NoSuchElementException();
            Object[] elementData = AbstractList.this.toArray();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }
        @Override public boolean hasPrevious() { return cursor != 0; }
        @Override public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0) throw new NoSuchElementException();
            Object[] elementData = AbstractList.this.toArray();
            cursor = i;
            return (E) elementData[lastRet = i];
        }
        @Override public int nextIndex() { return cursor; }
        @Override public int previousIndex() { return cursor - 1; }
        @Override public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            checkForComodification();
            try {
                AbstractList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
        @Override public void set(E e) {
            if (lastRet < 0) throw new IllegalStateException();
            checkForComodification();
            try {
                AbstractList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        @Override public void add(E e) {
            checkForComodification();
            int i = cursor;
            AbstractList.this.add(i, e);
            cursor = i + 1;
            lastRet = -1;
            expectedModCount = modCount;
        }
        private void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        ListIterator<E> it = listIterator();
        if (o == null) {
            while (it.hasNext()) {
                if (it.next() == null) return it.previousIndex();
            }
        } else {
            while (it.hasNext()) {
                if (o.equals(it.next())) return it.previousIndex();
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        ListIterator<E> it = listIterator(size());
        if (o == null) {
            while (it.hasPrevious()) {
                if (it.previous() == null) return it.nextIndex();
            }
        } else {
            while (it.hasPrevious()) {
                if (o.equals(it.previous())) return it.nextIndex();
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException();
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0) return false;
        for (int i = 0; i < numNew; i++) {
            add(index + i, (E) a[i]);
        }
        return true;
    }

    @Override
    public void replaceAll(java.util.function.UnaryOperator<E> operator) {
        ListIterator<E> li = this.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        java.util.Arrays.sort(a, (a1, a2) -> c.compare((E) a1, (E) a2));
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }

    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i = 0, n = toIndex - fromIndex; i < n; i++) {
            it.next();
            it.remove();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List)) return false;
        ListIterator<E> e1 = listIterator();
        ListIterator<?> e2 = ((List<?>) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return null;
    }
}
