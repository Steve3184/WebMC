package java.util;

public interface Iterable<T> {
    Iterator<T> iterator();
    default void forEach(java.util.function.Consumer<? super T> action) {
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            action.accept(it.next());
        }
    }
}
