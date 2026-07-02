package java.util.concurrent;

import java.util.NavigableMap;

public interface ConcurrentNavigableMap<K, V> extends ConcurrentMap<K, V>, NavigableMap<K, V> {
    @Override ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);
    @Override ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive);
    @Override ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);
    @Override ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey);
    @Override ConcurrentNavigableMap<K, V> headMap(K toKey);
    @Override ConcurrentNavigableMap<K, V> tailMap(K fromKey);
    @Override ConcurrentNavigableMap<K, V> descendingMap();
    @Override java.util.NavigableSet<K> navigableKeySet();
    @Override java.util.NavigableSet<K> keySet();
    @Override java.util.NavigableSet<K> descendingKeySet();
}
