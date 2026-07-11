package java.util;

public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    protected AbstractSequentialList() {}

    public Iterator<E> iterator() {
        return listIterator();
    }

    public abstract ListIterator<E> listIterator(int index);
}
