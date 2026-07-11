package java.util;

public abstract class AbstractMap<K, V> implements Map<K, V> {
    protected AbstractMap() {}

    @Override
    public void clear() {
        entrySet().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        Iterator<Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (e.getKey() == null) return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        Iterator<Entry<K, V>> i = entrySet().iterator();
        if (value == null) {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (e.getValue() == null) return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (value.equals(e.getValue())) return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map)) return false;
        return entrySet().equals(((Map<?, ?>) o).entrySet());
    }

    @Override
    public V get(Object key) {
        Iterator<Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (e.getKey() == null) return e.getValue();
            }
        } else {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) return e.getValue();
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        Iterator<Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (e.getKey() == null) {
                    i.remove();
                    return e.getValue();
                }
            }
        } else {
            while (i.hasNext()) {
                Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) {
                    i.remove();
                    return e.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        if (v != null || containsKey(key)) {
            return v;
        }
        return defaultValue;
    }

    @Override
    public V computeIfAbsent(Object key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        V v = get(key);
        if (v != null) {
            return v;
        }
        V newValue = mappingFunction.apply((K) key);
        if (newValue != null) {
            put((K) key, newValue);
        }
        return newValue;
    }

    @Override
    public Collection<V> values() {
        if (values == null) {
            values = new AbstractCollection<V>() {
                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        private Iterator<Entry<K, V>> i = entrySet().iterator();
                        @Override
                        public boolean hasNext() { return i.hasNext(); }
                        @Override
                        public V next() { return i.next().getValue(); }
                        @Override
                        public void remove() { i.remove(); }
                    };
                }
                @Override
                public int size() { return AbstractMap.this.size(); }
            };
        }
        return values;
    }

    private transient Collection<V> values;
    private transient Set<K> keySet;
    private transient Set<Entry<K, V>> entrySet;

    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {
                public Iterator<K> iterator() {
                    return new Iterator<K>() {
                        private Iterator<Entry<K, V>> i = entrySet().iterator();
                        @Override public boolean hasNext() { return i.hasNext(); }
                        @Override public K next() { return i.next().getKey(); }
                        @Override public void remove() { i.remove(); }
                    };
                }
                public int size() { return AbstractMap.this.size(); }
                public void clear() { AbstractMap.this.clear(); }
            };
        }
        return keySet;
    }

    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet<Entry<K, V>>() {
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<Entry<K, V>>() {
                        private int index = 0;
                        private int lastIndex = -1;
                        @Override public boolean hasNext() { return index < size(); }
                        @Override public Entry<K, V> next() {
                            if (index >= size()) throw new NoSuchElementException();
                            lastIndex = index++;
                            return entryAt(lastIndex);
                        }
                        @Override public void remove() {
                            if (lastIndex < 0) throw new IllegalStateException();
                            removeEntryAt(lastIndex);
                            lastIndex = -1;
                        }
                    };
                }
                public int size() { return AbstractMap.this.size(); }
                public void clear() { AbstractMap.this.clear(); }
            };
        }
        return entrySet;
    }

    protected Entry<K, V> entryAt(int index) { throw new UnsupportedOperationException(); }
    protected void removeEntryAt(int index) { throw new UnsupportedOperationException(); }

    protected void checkNotNull(Object key) {
        if (key == null) throw new NullPointerException();
    }
}
