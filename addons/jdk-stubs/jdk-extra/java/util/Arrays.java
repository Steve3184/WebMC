package java.util;

public class Arrays {
    private Arrays() {}

    public static <T> java.util.List<T> asList(T... a) {
        return new ArrayList<T>(a);
    }

    private static class ArrayList<T> extends java.util.AbstractList<T> {
        private final T[] a;
        ArrayList(T[] array) { a = array; }
        public int size() { return a.length; }
        public T get(int i) { return a[i]; }
        public T set(int i, T val) { T old = a[i]; a[i] = val; return old; }
    }

    public static int hashCode(Object a[]) {
        if (a == null) return 0;
        int result = 1;
        for (Object element : a) {
            result = 31 * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    public static boolean equals(Object a[], Object a2[]) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) {
            Object o1 = a[i];
            Object o2 = a2[i];
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return true;
    }

    public static boolean equals(byte a[], byte a2[]) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        if (a.length != a2.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != a2[i]) return false;
        }
        return true;
    }

    public static <T> void sort(T[] a, Comparator<? super T> c) {
        if (c == null) {
            sort(a);
        } else {
            sort(a, 0, a.length, c);
        }
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, Comparator<? super T> c) {
        if (c == null) {
            sort(a, fromIndex, toIndex);
        }
    }

    public static void sort(Object[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(Object[] a, int fromIndex, int toIndex) {
        // 简单的冒泡排序
        for (int i = fromIndex; i < toIndex - 1; i++) {
            for (int j = fromIndex; j < toIndex - i - 1 + fromIndex; j++) {
                if (((Comparable) a[j]).compareTo(a[j + 1]) > 0) {
                    Object temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                }
            }
        }
    }

    public static void sort(int[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(int[] a, int fromIndex, int toIndex) {
        // 简单的插入排序
        for (int i = fromIndex + 1; i < toIndex; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= fromIndex && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(long[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static void sort(double[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(double[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static void sort(float[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(float[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static void sort(byte[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(byte[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static void sort(char[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(char[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static void sort(short[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(short[] a, int fromIndex, int toIndex) {
        java.util.Arrays.sort(a, fromIndex, toIndex);
    }

    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return binarySearch(a, 0, a.length, key, c);
    }

    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = c.compare(a[mid], key);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static int binarySearch(Object[] a, Object key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(Object[] a, int fromIndex, int toIndex, Object key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            @SuppressWarnings("rawtypes")
            Comparable midVal = (Comparable) a[mid];
            int cmp = midVal.compareTo(key);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static int binarySearch(int[] a, int key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];
            if (midVal < key) low = mid + 1;
            else if (midVal > key) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static int binarySearch(long[] a, long key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(long[] a, int fromIndex, int toIndex, long key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid];
            if (midVal < key) low = mid + 1;
            else if (midVal > key) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static int binarySearch(double[] a, double key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(double[] a, int fromIndex, int toIndex, double key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = a[mid];
            if (midVal < key) low = mid + 1;
            else if (midVal > key) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        @SuppressWarnings("unchecked")
        T[] copy = (T[]) java.lang.reflect.Array.newInstance(original.getClass().getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = (T[]) java.lang.reflect.Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static short[] copyOf(short[] original, int newLength) {
        short[] copy = new short[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static long[] copyOf(long[] original, int newLength) {
        long[] copy = new long[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static char[] copyOf(char[] original, int newLength) {
        char[] copy = new char[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static float[] copyOf(float[] original, int newLength) {
        float[] copy = new float[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return copyOfRange(original, from, to, (Class<? extends T[]>) original.getClass());
    }

    public static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        @SuppressWarnings("unchecked")
        T[] copy = (T[]) java.lang.reflect.Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    public static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    public static long[] copyOfRange(long[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        long[] copy = new long[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    public static void fill(Object a[], Object val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(Object a[], int fromIndex, int toIndex, Object val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(int a[], int val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(int a[], int fromIndex, int toIndex, int val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(long a[], long val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(long a[], int fromIndex, int toIndex, long val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(double a[], double val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(double a[], int fromIndex, int toIndex, double val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(boolean a[], boolean val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(boolean a[], int fromIndex, int toIndex, boolean val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(byte a[], byte val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(byte a[], int fromIndex, int toIndex, byte val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(char a[], char val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(char a[], int fromIndex, int toIndex, char val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(short a[], short val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(short a[], int fromIndex, int toIndex, short val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static void fill(float a[], float val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(float a[], int fromIndex, int toIndex, float val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static boolean deepEquals(Object[] a1, Object[] a2) {
        if (a1 == a2) return true;
        if (a1 == null || a2 == null) return false;
        int length = a1.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];
            if (!Objects.equals(e1, e2)) return false;
        }
        return true;
    }

    public static int deepHashCode(Object a[]) {
        if (a == null) return 0;
        int result = 1;
        for (Object element : a) {
            result = 31 * result + (element == null ? 0 : deepHashCodeElement(element));
        }
        return result;
    }

    private static int deepHashCodeElement(Object elem) {
        if (elem == null) return 0;
        Class<?> cl = elem.getClass();
        if (cl.isArray()) {
            if (elem instanceof Object[]) return deepHashCode((Object[]) elem);
            if (elem instanceof int[]) return hashCode((int[]) elem);
            if (elem instanceof long[]) return hashCode((long[]) elem);
            if (elem instanceof short[]) return hashCode((short[]) elem);
            if (elem instanceof byte[]) return hashCode((byte[]) elem);
            if (elem instanceof double[]) return hashCode((double[]) elem);
            if (elem instanceof float[]) return hashCode((float[]) elem);
            if (elem instanceof boolean[]) return hashCode((boolean[]) elem);
            if (elem instanceof char[]) return hashCode((char[]) elem);
        }
        return elem.hashCode();
    }

    public static int hashCode(boolean a[]) {
        if (a == null) return 0;
        int result = 1;
        for (boolean element : a) {
            result = 31 * result + (element ? 1231 : 1237);
        }
        return result;
    }

    public static int hashCode(byte a[]) {
        if (a == null) return 0;
        int result = 1;
        for (byte element : a) {
            result = 31 * result + element;
        }
        return result;
    }

    public static int hashCode(short a[]) {
        if (a == null) return 0;
        int result = 1;
        for (short element : a) {
            result = 31 * result + element;
        }
        return result;
    }

    public static int hashCode(char a[]) {
        if (a == null) return 0;
        int result = 1;
        for (char element : a) {
            result = 31 * result + element;
        }
        return result;
    }

    public static int hashCode(int a[]) {
        if (a == null) return 0;
        int result = 1;
        for (int element : a) {
            result = 31 * result + element;
        }
        return result;
    }

    public static int hashCode(long a[]) {
        if (a == null) return 0;
        int result = 1;
        for (long element : a) {
            result = 31 * result + (int)(element ^ (element >>> 32));
        }
        return result;
    }

    public static int hashCode(float a[]) {
        if (a == null) return 0;
        int result = 1;
        for (float element : a) {
            result = 31 * result + Float.hashCode(element);
        }
        return result;
    }

    public static int hashCode(double a[]) {
        if (a == null) return 0;
        int result = 1;
        for (double element : a) {
            result = 31 * result + Double.hashCode(element);
        }
        return result;
    }

    public static String toString(Object a[]) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) return b.append(']').toString();
            b.append(", ");
        }
    }

    public static String toString(int a[]) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) return b.append(']').toString();
            b.append(", ");
        }
    }

    public static String toString(long a[]) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) return b.append(']').toString();
            b.append(", ");
        }
    }

    public static <T> java.util.List<T> list(java.util.List<T> e) {
        return e;
    }
}
