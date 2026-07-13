package java.util;

public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
    private final Comparator<? super K> comparator;
    private Node<K, V> root;
    private int size = 0;
    private int modCount = 0;

    public TreeMap() {
        comparator = null;
    }

    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
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
    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Node<K, V> n = getFirstNode(); n != null; n = successor(n)) {
            if (valEquals(value, n.value)) return true;
        }
        return false;
    }

    @Override
    public V get(Object key) {
        Node<K, V> n = getNode(key);
        return n == null ? null : n.value;
    }

    final Node<K, V> getNode(Object key) {
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        Node<K, V> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.key);
            if (cmp < 0) p = p.left;
            else if (cmp > 0) p = p.right;
            else return p;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        Node<K, V> t = root;
        if (t == null) {
            root = new Node<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Node<K, V> parent;
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0) t = t.left;
            else if (cmp > 0) t = t.right;
            else return t.setValue(value);
        } while (t != null);
        Node<K, V> e = new Node<>(key, value, parent);
        if (cmp < 0) parent.left = e;
        else parent.right = e;
        size++;
        modCount++;
        return null;
    }

    @Override
    public V remove(Object key) {
        Node<K, V> n = getNode(key);
        if (n == null) return null;
        V oldValue = n.value;
        deleteNode(n);
        return oldValue;
    }

    private void deleteNode(Node<K, V> p) {
        size--;
        modCount++;
        if (p.left != null && p.right != null) {
            Node<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        }
        Node<K, V> replacement = (p.left != null) ? p.left : p.right;
        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null) root = replacement;
            else if (p == p.parent.left) p.parent.left = replacement;
            else p.parent.right = replacement;
        } else if (p.parent == null) {
            root = null;
        } else {
            if (p == p.parent.left) p.parent.left = null;
            else p.parent.right = null;
            p.parent = null;
        }
    }

    @Override
    public void clear() {
        size = 0;
        modCount++;
        root = null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public int size() { return size; }
            @Override
            public void clear() { TreeMap.this.clear(); }
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new TreeMapIterator();
            }
        };
    }

    Node<K, V> getFirstNode() {
        Node<K, V> n = root;
        if (n != null) while (n.left != null) n = n.left;
        return n;
    }

    static <K, V> Node<K, V> successor(Node<K, V> t) {
        if (t == null) return null;
        if (t.right != null) {
            Node<K, V> p = t.right;
            while (p.left != null) p = p.left;
            return p;
        }
        Node<K, V> p = t.parent;
        while (p != null && t == p.right) { t = p; p = p.parent; }
        return p;
    }

    static final class Node<K, V> implements Entry<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> parent;
        Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
        public K getKey() { return key; }
        public V getValue() { return value; }
        public V setValue(V value) { V oldValue = this.value; this.value = value; return oldValue; }
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public int size() { return size; }
            @Override
            public void clear() { TreeMap.this.clear(); }
            @Override
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private Node<K, V> next = getFirstNode();

                    @Override
                    public boolean hasNext() { return next != null; }

                    @Override
                    public K next() {
                        if (next == null) throw new NoSuchElementException();
                        K key = next.key;
                        next = successor(next);
                        return key;
                    }
                };
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public int size() { return size; }
            @Override
            public void clear() { TreeMap.this.clear(); }
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private Node<K, V> next = getFirstNode();

                    @Override
                    public boolean hasNext() { return next != null; }

                    @Override
                    public V next() {
                        if (next == null) throw new NoSuchElementException();
                        V value = next.value;
                        next = successor(next);
                        return value;
                    }
                };
            }
        };
    }

    class TreeMapIterator implements Iterator<Entry<K, V>> {
        private Node<K, V> next;
        private Node<K, V> lastReturned;
        TreeMapIterator() { next = getFirstNode(); }
        public boolean hasNext() { return next != null; }
        public Entry<K, V> next() {
            if (next == null) throw new NoSuchElementException();
            lastReturned = next;
            next = successor(next);
            return lastReturned;  // Node implements Entry
        }
        public void remove() {
            if (lastReturned == null) throw new IllegalStateException();
            deleteNode(lastReturned);
            lastReturned = null;
        }
    }

    private static boolean valEquals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    @Override
    public K firstKey() { return getFirstNode().key; }
    @Override
    public K lastKey() { return getLastNode().key; }
    Node<K, V> getLastNode() { Node<K, V> n = root; if (n != null) while (n.right != null) n = n.right; return n; }
    @Override public Entry<K, V> lowerEntry(K key) { return null; }
    @Override public K lowerKey(K key) { return null; }
    @Override public Entry<K, V> floorEntry(K key) { return null; }
    @Override public K floorKey(K key) { return null; }
    @Override public Entry<K, V> ceilingEntry(K key) { return null; }
    @Override public K ceilingKey(K key) { return null; }
    @Override public Entry<K, V> higherEntry(K key) { return null; }
    @Override public K higherKey(K key) { return null; }
    @Override public Entry<K, V> firstEntry() { return null; }
    @Override public Entry<K, V> lastEntry() { return null; }
    @Override public Entry<K, V> pollFirstEntry() { return null; }
    @Override public Entry<K, V> pollLastEntry() { return null; }
    @Override public NavigableSet<K> navigableKeySet() { return null; }
    @Override public NavigableSet<K> descendingKeySet() { return null; }
    @Override public NavigableMap<K, V> descendingMap() { return null; }
    @Override public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) { return null; }
    @Override public NavigableMap<K, V> headMap(K toKey, boolean inclusive) { return null; }
    @Override public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) { return null; }
    @Override public SortedMap<K, V> subMap(K fromKey, K toKey) { return null; }
    @Override public SortedMap<K, V> headMap(K toKey) { return null; }
    @Override public SortedMap<K, V> tailMap(K fromKey) { return null; }
    @Override public Comparator<? super K> comparator() { return comparator; }
}
