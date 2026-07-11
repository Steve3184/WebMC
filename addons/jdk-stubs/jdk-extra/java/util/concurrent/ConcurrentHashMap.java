package java.util.concurrent;

import java.util.AbstractMap;
import java.util.Set;
import java.util.Collection;
import java.util.Map;

public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    private final Map<K, V> map = new java.util.HashMap<>();

    public ConcurrentHashMap() {}

    @Override
    public V get(Object key) { return map.get(key); }

    @Override
    public V put(K key, V value) { return map.put(key, value); }

    @Override
    public V remove(Object key) { return map.remove(key); }

    @Override
    public boolean containsKey(Object key) { return map.containsKey(key); }

    @Override
    public void clear() { map.clear(); }

    @Override
    public int size() { return map.size(); }

    @Override
    public Set<Entry<K, V>> entrySet() { return map.entrySet(); }

    public V putIfAbsent(K key, V value) {
        if (!map.containsKey(key)) {
            return map.put(key, value);
        }
        return map.get(key);
    }

    public boolean remove(Object key, Object value) {
        if (map.containsKey(key) && java.util.Objects.equals(map.get(key), value)) {
            map.remove(key);
            return true;
        }
        return false;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (map.containsKey(key) && java.util.Objects.equals(map.get(key), oldValue)) {
            map.put(key, newValue);
            return true;
        }
        return false;
    }

    public V replace(K key, V value) {
        if (map.containsKey(key)) {
            return map.put(key, value);
        }
        return null;
    }

    @Override
    public V computeIfAbsent(Object key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        V v = map.get(key);
        if (v == null) {
            v = mappingFunction.apply((K) key);
            if (v != null) {
                map.put((K) key, v);
            }
        }
        return v;
    }

    public V computeIfPresent(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V v = map.get(key);
        if (v != null) {
            V newV = remappingFunction.apply(key, v);
            if (newV != null) {
                map.put(key, newV);
                return newV;
            } else {
                map.remove(key);
            }
        }
        return null;
    }
}
