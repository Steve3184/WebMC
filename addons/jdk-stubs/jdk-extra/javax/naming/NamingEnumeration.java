package javax.naming;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface NamingEnumeration<T> extends Iterator<T>, AutoCloseable {
    boolean hasMore();
    T next();
    default void remove() { throw new UnsupportedOperationException(); }
    default void close() {}
}
