package java.util;

public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
    private static final int DEFAULT_CAPACITY = 32;
    private static final int MAX_CAPACITY = 1 << 29;
    private Object[] table;
    private int size;

    public IdentityHashMap() {
        init(DEFAULT_CAPACITY);
    }

    public IdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) throw new IllegalArgumentException();
        init(capacity(expectedMaxSize));
    }

    public IdentityHashMap(Map<? extends K, ? extends V> m) {
        init(capacity(m.size()));
        putAll(m);
    }

    private static int capacity(int expectedMaxSize) {
        return (expectedMaxSize > (MAX_CAPACITY / 3)) ? MAX_CAPACITY : (expectedMaxSize * 3) + 1;
    }

    private void init(int initCapacity) {
        table = new Object[initCapacity * 2];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public V get(Object key) {
        Object k = maskNull(key);
        int i = hash(k);
        Object[] tab = table;
        int len = tab.length;
        while (true) {
            Object item = tab[i];
            if (item == k) return (V) tab[i + 1];
            if (item == null) return null;
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        Object k = maskNull(key);
        int i = hash(k);
        Object[] tab = table;
        int len = tab.length;
        while (true) {
            Object item = tab[i];
            if (item == k) return true;
            if (item == null) return false;
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public V put(K key, V value) {
        Object k = maskNull(key);
        int i = hash(k);
        Object[] tab = table;
        int len = tab.length;
        while (true) {
            Object item = tab[i];
            if (item == k) {
                V oldValue = (V) tab[i + 1];
                tab[i + 1] = value;
                return oldValue;
            }
            if (item == null) {
                tab[i] = k;
                tab[i + 1] = value;
                size++;
                return null;
            }
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public void clear() {
        Object[] tab = table;
        for (int i = 0; i < tab.length; i++) tab[i] = null;
        size = 0;
    }

    @Override
    public V remove(Object key) {
        Object k = maskNull(key);
        int i = hash(k);
        Object[] tab = table;
        int len = tab.length;
        while (true) {
            Object item = tab[i];
            if (item == k) {
                V oldValue = (V) tab[i + 1];
                int next = i + 2 < len ? i + 2 : 0;
                while (tab[next] != null) {
                    int r = nextKeyIndex(next, len);
                    tab[i] = tab[next];
                    tab[i + 1] = tab[next + 1];
                    i = nextKeyIndex(i, len);
                    next = r;
                }
                tab[i] = null;
                tab[i + 1] = null;
                size--;
                return oldValue;
            }
            if (item == null) return null;
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    protected Entry<K, V> entryAt(int index) {
        if (index >= size * 2) throw new IndexOutOfBoundsException();
        Object[] tab = table;
        int count = 0;
        for (int i = 0; i < tab.length; i += 2) {
            if (tab[i] != null) {
                if (count == index) {
                    return new SimpleEntry<>((K) tab[i], (V) tab[i + 1]);
                }
                count++;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    protected void removeEntryAt(int index) {
        remove(entryAt(index).getKey());
    }

    private int hash(Object k) {
        int h = System.identityHashCode(k);
        return (h << 1) - (h << Integer.SIZE - 1) & (table.length - 1);
    }

    private static int nextKeyIndex(int i, int len) {
        return (i + 2 < len) ? i + 2 : 0;
    }

    private static Object maskNull(Object key) {
        return key == null ? NULL_KEY : key;
    }

    private static final Object NULL_KEY = new Object();

    private static class SimpleEntry<K, V> implements Entry<K, V> {
        private final K key;
        private V value;
        SimpleEntry(K key, V value) { this.key = key; this.value = value; }
        public K getKey() { return key; }
        public V getValue() { return value; }
        public V setValue(V value) { V old = this.value; this.value = value; return old; }
    }
}
