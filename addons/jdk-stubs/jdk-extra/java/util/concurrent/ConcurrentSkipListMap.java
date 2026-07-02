package java.util.concurrent;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** Single-threaded JS implementation of ConcurrentSkipListMap backed by TreeMap. */
public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V> {
    private final TreeMap<K, V> backing;
    public ConcurrentSkipListMap() { this.backing = new TreeMap<>(); }
    public ConcurrentSkipListMap(Comparator<? super K> comparator) { this.backing = new TreeMap<>(comparator); }
    public ConcurrentSkipListMap(java.util.Map<? extends K, ? extends V> m) { this.backing = new TreeMap<>(m); }
    public ConcurrentSkipListMap(SortedMap<K, ? extends V> m) { this.backing = new TreeMap<>(m); }

    @Override public V get(Object key) { return backing.get(key); }
    @Override public V put(K key, V value) { return backing.put(key, value); }
    @Override public V remove(Object key) { return backing.remove(key); }
    @Override public boolean containsKey(Object key) { return backing.containsKey(key); }
    @Override public int size() { return backing.size(); }
    @Override public Set<Entry<K, V>> entrySet() { return backing.entrySet(); }
    @Override public void clear() { backing.clear(); }

    @Override public V putIfAbsent(K key, V value) { return backing.putIfAbsent(key, value); }
    @Override public boolean remove(Object key, Object value) { return backing.remove(key, value); }
    @Override public boolean replace(K key, V oldValue, V newValue) { return backing.replace(key, oldValue, newValue); }
    @Override public V replace(K key, V value) { return backing.replace(key, value); }

    @Override public Comparator<? super K> comparator() { return backing.comparator(); }
    @Override public K firstKey() { return backing.firstKey(); }
    @Override public K lastKey() { return backing.lastKey(); }
    @Override public Entry<K, V> firstEntry() { return backing.firstEntry(); }
    @Override public Entry<K, V> lastEntry() { return backing.lastEntry(); }
    @Override public Entry<K, V> pollFirstEntry() { return backing.pollFirstEntry(); }
    @Override public Entry<K, V> pollLastEntry() { return backing.pollLastEntry(); }
    @Override public Entry<K, V> lowerEntry(K key) { return backing.lowerEntry(key); }
    @Override public K lowerKey(K key) { return backing.lowerKey(key); }
    @Override public Entry<K, V> floorEntry(K key) { return backing.floorEntry(key); }
    @Override public K floorKey(K key) { return backing.floorKey(key); }
    @Override public Entry<K, V> ceilingEntry(K key) { return backing.ceilingEntry(key); }
    @Override public K ceilingKey(K key) { return backing.ceilingKey(key); }
    @Override public Entry<K, V> higherEntry(K key) { return backing.higherEntry(key); }
    @Override public K higherKey(K key) { return backing.higherKey(key); }

    @Override public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new ConcurrentSkipListMap<>(backing.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }
    @Override public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new ConcurrentSkipListMap<>(backing.headMap(toKey, inclusive));
    }
    @Override public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new ConcurrentSkipListMap<>(backing.tailMap(fromKey, inclusive));
    }
    @Override public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) { return subMap(fromKey, true, toKey, false); }
    @Override public ConcurrentNavigableMap<K, V> headMap(K toKey) { return headMap(toKey, false); }
    @Override public ConcurrentNavigableMap<K, V> tailMap(K fromKey) { return tailMap(fromKey, true); }
    @Override public ConcurrentNavigableMap<K, V> descendingMap() {
        return new ConcurrentSkipListMap<>(backing.descendingMap());
    }
    @Override public NavigableSet<K> navigableKeySet() { return backing.navigableKeySet(); }
    @Override public NavigableSet<K> keySet() { return backing.navigableKeySet(); }
    @Override public NavigableSet<K> descendingKeySet() { return backing.descendingKeySet(); }
}
