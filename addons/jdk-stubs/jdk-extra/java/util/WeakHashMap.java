package java.util;

public class WeakHashMap<K, V> extends AbstractMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private java.util.HashMap<K, java.lang.ref.WeakReference<V>> backing;

    public WeakHashMap() {
        backing = new HashMap<>();
    }

    public WeakHashMap(int initialCapacity) {
        backing = new HashMap<>(initialCapacity);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    @Override
    public V get(Object key) {
        java.lang.ref.WeakReference<V> ref = backing.get(key);
        return ref != null ? ref.get() : null;
    }

    @Override
    public V put(K key, V value) {
        java.lang.ref.WeakReference<V> oldRef = backing.get(key);
        backing.put(key, new java.lang.ref.WeakReference<>(value));
        return oldRef != null ? oldRef.get() : null;
    }

    @Override
    public V remove(Object key) {
        java.lang.ref.WeakReference<V> ref = backing.remove(key);
        return ref != null ? ref.get() : null;
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    protected Entry<K, V> entryAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void removeEntryAt(int index) {
        throw new UnsupportedOperationException();
    }
}
