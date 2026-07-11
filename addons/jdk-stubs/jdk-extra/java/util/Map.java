package java.util;

public interface Map<K, V> {
    int size();
    boolean isEmpty();
    boolean containsKey(Object key);
    boolean containsValue(Object value);
    V get(Object key);
    V put(K key, V value);
    V remove(Object key);
    void putAll(Map<? extends K, ? extends V> m);
    void clear();
    Set<K> keySet();
    Collection<V> values();
    Set<Entry<K, V>> entrySet();
    boolean equals(Object o);
    int hashCode();
    V getOrDefault(Object key, V defaultValue);
    V computeIfAbsent(Object key, java.util.function.Function<? super K, ? extends V> mappingFunction);

    static <K, V> Map<K, V> of() { return new EmptyMap<>(); }
    static <K, V> Map<K, V> of(K k1, V v1) { return new SingleEntryMap<>(k1, v1); }
    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) { return new TwoEntryMap<>(k1, v1, k2, v2); }
    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) { return new ThreeEntryMap<>(k1, v1, k2, v2, k3, v3); }

    interface Entry<K, V> {
        K getKey();
        V getValue();
        V setValue(V value);
        boolean equals(Object o);
        int hashCode();
    }

    class EmptyMap<K, V> implements Map<K, V> {
        public int size() { return 0; }
        public boolean isEmpty() { return true; }
        public boolean containsKey(Object k) { return false; }
        public boolean containsValue(Object v) { return false; }
        public V get(Object k) { return null; }
        public V put(K k, V v) { throw new UnsupportedOperationException(); }
        public V remove(Object k) { return null; }
        public void putAll(Map<? extends K, ? extends V> m) {}
        public void clear() {}
        public Set<K> keySet() { return Collections.emptySet(); }
        public Collection<V> values() { return Collections.emptySet(); }
        public Set<Entry<K, V>> entrySet() { return Collections.emptySet(); }
        public boolean equals(Object o) { return o instanceof Map && ((Map<?, ?>) o).isEmpty(); }
        public int hashCode() { return 0; }
        public V getOrDefault(Object k, V v) { return v; }
        public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) { return f.apply((K) k); }
    }

    class SingleEntryMap<K, V> implements Map<K, V> {
        private final K k1;
        private final V v1;
        SingleEntryMap(K k1, V v1) { this.k1 = k1; this.v1 = v1; }
        public int size() { return 1; }
        public boolean isEmpty() { return false; }
        public boolean containsKey(Object k) { return k == null ? k1 == null : k.equals(k1); }
        public boolean containsValue(Object v) { return v == null ? v1 == null : v.equals(v1); }
        public V get(Object k) { return containsKey(k) ? v1 : null; }
        public V put(K k, V v) { throw new UnsupportedOperationException(); }
        public V remove(Object k) { return containsKey(k) ? v1 : null; }
        public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }
        public void clear() { throw new UnsupportedOperationException(); }
        public Set<K> keySet() { return new SingletonSet<>(k1); }
        public Collection<V> values() { return new SingletonCollection<>(v1); }
        public Set<Entry<K, V>> entrySet() { return new SingletonSet<>(new SimpleEntry<>(k1, v1)); }
        public boolean equals(Object o) { return o instanceof Map && ((Map<?, ?>) o).size() == 1 && containsKey(((Map<?, ?>) o).keySet().iterator().next()); }
        public int hashCode() { return (k1 == null ? 0 : k1.hashCode()) ^ (v1 == null ? 0 : v1.hashCode()); }
        public V getOrDefault(Object k, V v) { return containsKey(k) ? v1 : v; }
        public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) {
            if (containsKey(k)) return get(k);
            V newV = f.apply((K) k);
            return newV;
        }
    }

    class TwoEntryMap<K, V> implements Map<K, V> {
        private final K k1, k2;
        private final V v1, v2;
        TwoEntryMap(K k1, V v1, K k2, V v2) { this.k1 = k1; this.v1 = v1; this.k2 = k2; this.v2 = v2; }
        public int size() { return 2; }
        public boolean isEmpty() { return false; }
        public boolean containsKey(Object k) { return (k == null ? k1 == null : k.equals(k1)) || (k == null ? k2 == null : k.equals(k2)); }
        public boolean containsValue(Object v) { return (v == null ? v1 == null : v.equals(v1)) || (v == null ? v2 == null : v.equals(v2)); }
        public V get(Object k) {
            if (k == null ? k1 == null : k.equals(k1)) return v1;
            if (k == null ? k2 == null : k.equals(k2)) return v2;
            return null;
        }
        public V put(K k, V v) { throw new UnsupportedOperationException(); }
        public V remove(Object k) {
            if (k == null ? k1 == null : k.equals(k1)) return v1;
            if (k == null ? k2 == null : k.equals(k2)) return v2;
            return null;
        }
        public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }
        public void clear() { throw new UnsupportedOperationException(); }
        public Set<K> keySet() { return new TwoElementSet<>(k1, k2); }
        public Collection<V> values() { return new TwoElementCollection<>(v1, v2); }
        public Set<Entry<K, V>> entrySet() { return new TwoElementSet<>(new SimpleEntry<>(k1, v1), new SimpleEntry<>(k2, v2)); }
        public boolean equals(Object o) { return o instanceof Map && ((Map<?, ?>) o).size() == 2 && containsKey(((Map<?, ?>) o).keySet().iterator().next()); }
        public int hashCode() { return (k1 == null ? 0 : k1.hashCode()) ^ (v1 == null ? 0 : v1.hashCode()) ^ (k2 == null ? 0 : k2.hashCode()) ^ (v2 == null ? 0 : v2.hashCode()); }
        public V getOrDefault(Object k, V v) { return containsKey(k) ? get(k) : v; }
        public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) {
            if (containsKey(k)) return get(k);
            V newV = f.apply((K) k);
            return newV;
        }
    }

    class ThreeEntryMap<K, V> implements Map<K, V> {
        private final K k1, k2, k3;
        private final V v1, v2, v3;
        ThreeEntryMap(K k1, V v1, K k2, V v2, K k3, V v3) { this.k1 = k1; this.v1 = v1; this.k2 = k2; this.v2 = v2; this.k3 = k3; this.v3 = v3; }
        public int size() { return 3; }
        public boolean isEmpty() { return false; }
        public boolean containsKey(Object k) { return (k == null ? k1 == null : k.equals(k1)) || (k == null ? k2 == null : k.equals(k2)) || (k == null ? k3 == null : k.equals(k3)); }
        public boolean containsValue(Object v) { return (v == null ? v1 == null : v.equals(v1)) || (v == null ? v2 == null : v.equals(v2)) || (v == null ? v3 == null : v.equals(v3)); }
        public V get(Object k) {
            if (k == null ? k1 == null : k.equals(k1)) return v1;
            if (k == null ? k2 == null : k.equals(k2)) return v2;
            if (k == null ? k3 == null : k.equals(k3)) return v3;
            return null;
        }
        public V put(K k, V v) { throw new UnsupportedOperationException(); }
        public V remove(Object k) {
            if (k == null ? k1 == null : k.equals(k1)) return v1;
            if (k == null ? k2 == null : k.equals(k2)) return v2;
            if (k == null ? k3 == null : k.equals(k3)) return v3;
            return null;
        }
        public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }
        public void clear() { throw new UnsupportedOperationException(); }
        public Set<K> keySet() { return new ThreeElementSet<>(k1, k2, k3); }
        public Collection<V> values() { return new ThreeElementCollection<>(v1, v2, v3); }
        public Set<Entry<K, V>> entrySet() { return new ThreeElementSet<>(new SimpleEntry<>(k1, v1), new SimpleEntry<>(k2, v2), new SimpleEntry<>(k3, v3)); }
        public boolean equals(Object o) { return o instanceof Map && ((Map<?, ?>) o).size() == 3; }
        public int hashCode() { return (k1 == null ? 0 : k1.hashCode()) ^ (v1 == null ? 0 : v1.hashCode()) ^ (k2 == null ? 0 : k2.hashCode()) ^ (v2 == null ? 0 : v2.hashCode()) ^ (k3 == null ? 0 : k3.hashCode()) ^ (v3 == null ? 0 : v3.hashCode()); }
        public V getOrDefault(Object k, V v) { return containsKey(k) ? get(k) : v; }
        public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) {
            if (containsKey(k)) return get(k);
            V newV = f.apply((K) k);
            return newV;
        }
    }

    class SimpleEntry<K, V> implements Entry<K, V> {
        private final K key;
        private V value;
        SimpleEntry(K key, V value) { this.key = key; this.value = value; }
        public K getKey() { return key; }
        public V getValue() { return value; }
        public V setValue(V v) { V old = value; value = v; return old; }
        public boolean equals(Object o) { return o instanceof Entry && Objects.equals(key, ((Entry<?, ?>) o).getKey()) && Objects.equals(value, ((Entry<?, ?>) o).getValue()); }
        public int hashCode() { return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode()); }
    }

    class SingletonSet<E> extends AbstractSet<E> {
        private final E element;
        SingletonSet(E element) { this.element = element; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private boolean hasNext = true; public boolean hasNext() { return hasNext; } public E next() { hasNext = false; return element; } }; }
        public int size() { return 1; }
    }

    class TwoElementSet<E> extends AbstractSet<E> {
        private final E e1, e2;
        TwoElementSet(E e1, E e2) { this.e1 = e1; this.e2 = e2; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private int idx = 0; public boolean hasNext() { return idx < 2; } public E next() { return idx++ == 0 ? e1 : e2; } }; }
        public int size() { return 2; }
    }

    class ThreeElementSet<E> extends AbstractSet<E> {
        private final E e1, e2, e3;
        ThreeElementSet(E e1, E e2, E e3) { this.e1 = e1; this.e2 = e2; this.e3 = e3; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private int idx = 0; public boolean hasNext() { return idx < 3; } public E next() { return idx++ == 0 ? e1 : (idx == 1 ? e2 : e3); } }; }
        public int size() { return 3; }
    }

    class SingletonCollection<E> extends AbstractCollection<E> {
        private final E element;
        SingletonCollection(E element) { this.element = element; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private boolean hasNext = true; public boolean hasNext() { return hasNext; } public E next() { hasNext = false; return element; } }; }
        public int size() { return 1; }
    }

    class TwoElementCollection<E> extends AbstractCollection<E> {
        private final E e1, e2;
        TwoElementCollection(E e1, E e2) { this.e1 = e1; this.e2 = e2; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private int idx = 0; public boolean hasNext() { return idx < 2; } public E next() { return idx++ == 0 ? e1 : e2; } }; }
        public int size() { return 2; }
    }

    class ThreeElementCollection<E> extends AbstractCollection<E> {
        private final E e1, e2, e3;
        ThreeElementCollection(E e1, E e2, E e3) { this.e1 = e1; this.e2 = e2; this.e3 = e3; }
        public java.util.Iterator<E> iterator() { return new java.util.Iterator<E>() { private int idx = 0; public boolean hasNext() { return idx < 3; } public E next() { return idx++ == 0 ? e1 : (idx == 1 ? e2 : e3); } }; }
        public int size() { return 3; }
    }
}
