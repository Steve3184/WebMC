package java.util;

import java.util.function.Consumer;

public abstract class AbstractCollection<E> implements Collection<E> {
    protected AbstractCollection() {}

    @Override
    public abstract int size();

    @Override
    public abstract Iterator<E> iterator();

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            for (E e : this) {
                if (e == null) return true;
            }
        } else {
            for (E e : this) {
                if (o.equals(e)) return true;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size()];
        int i = 0;
        for (E e : this) {
            array[i++] = e;
        }
        return array;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        int i = 0;
        for (E e : this) {
            a[i++] = (T) e;
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        Iterator<E> i = iterator();
        if (o == null) {
            while (i.hasNext()) {
                if (i.next() == null) {
                    i.remove();
                    return true;
                }
            }
        } else {
            while (i.hasNext()) {
                if (o.equals(i.next())) {
                    i.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            add(e);
            modified = true;
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            if (c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            if (!c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            i.next();
            i.remove();
        }
    }

    @Override
    public String toString() {
        Iterator<E> i = iterator();
        if (!i.hasNext()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (i.hasNext()) {
            E e = i.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!i.hasNext()) return sb.append(']').toString();
            sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        for (E e : this) {
            action.accept(e);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return null;
    }
}
