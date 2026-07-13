package java.util;

public class Objects {
    private Objects() {}

    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static boolean deepEquals(Object a, Object b) {
        return equals(a, b);
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static int hash(Object... values) {
        return java.util.Arrays.hashCode(values);
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) throw new NullPointerException();
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) throw new NullPointerException(message);
        return obj;
    }

    public static <T> T requireNonNull(T obj, java.util.function.Supplier<String> messageSupplier) {
        if (obj == null) throw new NullPointerException(messageSupplier.get());
        return obj;
    }

    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return obj != null ? obj : requireNonNull(defaultObj, "defaultObj is null");
    }

    public static <T> T requireNonNullElseGet(T obj, java.util.function.Supplier<? extends T> defaultSupplier) {
        T temp = obj != null ? obj : requireNonNull(Objects.requireNonNull(defaultSupplier).get());
        return temp;
    }

    public static String toString(Object o) {
        return o == null ? "null" : o.toString();
    }

    public static String toString(Object o, String nullDefault) {
        return o == null ? nullDefault : o.toString();
    }

    public static int compare(Object a, Object b, java.util.Comparator<? super Object> c) {
        return c == null ? ((Comparable) a).compareTo(b) : c.compare(a, b);
    }

    public static <T> int checkIndex(int index, int length) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException("Index out of range: [" + index + "], length: " + length);
        return index;
    }

    public static <T> int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static <T> int checkFromIndexSize(int fromIndex, int size, int length) {
        return checkFromToIndex(fromIndex, fromIndex + size, length);
    }

    public static <T> T cast(Object obj) {
        return (T) obj;
    }
}
