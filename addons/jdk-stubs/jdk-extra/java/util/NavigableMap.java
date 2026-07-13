package java.util;

public interface NavigableMap<K, V> extends SortedMap<K, V> {
    Entry<K, V> lowerEntry(K key);
    K lowerKey(K key);
    Entry<K, V> floorEntry(K key);
    K floorKey(K key);
    Entry<K, V> ceilingEntry(K key);
    K ceilingKey(K key);
    Entry<K, V> higherEntry(K key);
    K higherKey(K key);
    Entry<K, V> firstEntry();
    Entry<K, V> lastEntry();
    Entry<K, V> pollFirstEntry();
    Entry<K, V> pollLastEntry();
    NavigableMap<K, V> descendingMap();
    NavigableSet<K> navigableKeySet();
    NavigableSet<K> descendingKeySet();
    NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);
    NavigableMap<K, V> headMap(K toKey, boolean inclusive);
    NavigableMap<K, V> tailMap(K fromKey, boolean inclusive);
}
