package java.util;

public class Collections {
    private Collections() {}

    public static <T> Set<T> unmodifiableSet(Set<T> s) {
        return new UnmodifiableSet<>(s);
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return new UnmodifiableSortedSet<>(s);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<K, V> m) {
        return new UnmodifiableMap<>(m);
    }

    public static <T> List<T> unmodifiableList(List<T> list) {
        return new UnmodifiableList<>(list);
    }

    public static <T> Collection<T> unmodifiableCollection(Collection<T> c) {
        return new UnmodifiableCollection<>(c);
    }

    public static <T> Iterator<T> unmodifiableIterator(Iterator<T> i) {
        return new UnmodifiableIterator<>(i);
    }

    public static <T> ListIterator<T> unmodifiableListIterator(ListIterator<T> i) {
        return new UnmodifiableListIterator<>(i);
    }

    public static <T> Enumeration<T> unmodifiableEnumeration(Enumeration<T> e) {
        return new UnmodifiableEnumeration<>(e);
    }

    private static class UnmodifiableSet<T> implements Set<T> {
        private final Set<T> s;

        UnmodifiableSet(Set<T> s) {
            this.s = s;
        }

        @Override public int size() { return s.size(); }
        @Override public boolean isEmpty() { return s.isEmpty(); }
        @Override public boolean contains(Object o) { return s.contains(o); }
        @Override public Object[] toArray() { return s.toArray(); }
        @Override public <T2> T2[] toArray(T2[] a) { return s.toArray(a); }
        @Override public Iterator<T> iterator() { return unmodifiableIterator(s.iterator()); }
        @Override public boolean add(T e) { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(Collection<?> c) { return s.containsAll(c); }
        @Override public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public boolean equals(Object o) { return s.equals(o); }
        @Override public int hashCode() { return s.hashCode(); }
        @Override public Spliterator<T> spliterator() { return s.spliterator(); }
    }

    private static class UnmodifiableSortedSet<T> implements SortedSet<T> {
        private final SortedSet<T> s;

        UnmodifiableSortedSet(SortedSet<T> s) {
            this.s = s;
        }

        @Override public int size() { return s.size(); }
        @Override public boolean isEmpty() { return s.isEmpty(); }
        @Override public boolean contains(Object o) { return s.contains(o); }
        @Override public Object[] toArray() { return s.toArray(); }
        @Override public <T2> T2[] toArray(T2[] a) { return s.toArray(a); }
        @Override public Iterator<T> iterator() { return unmodifiableIterator(s.iterator()); }
        @Override public boolean add(T e) { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(Collection<?> c) { return s.containsAll(c); }
        @Override public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public boolean equals(Object o) { return s.equals(o); }
        @Override public int hashCode() { return s.hashCode(); }

        @Override public Comparator<? super T> comparator() { return s.comparator(); }
        @Override public T first() { return s.first(); }
        @Override public T last() { return s.last(); }
        @Override public SortedSet<T> subSet(T from, T to) { return s.subSet(from, to); }
        @Override public SortedSet<T> headSet(T to) { return s.headSet(to); }
        @Override public SortedSet<T> tailSet(T from) { return s.tailSet(from); }
        @Override public Spliterator<T> spliterator() { return s.spliterator(); }
    }

    private static class UnmodifiableMap<K, V> implements Map<K, V> {
        private final Map<K, V> m;

        UnmodifiableMap(Map<K, V> m) {
            this.m = m;
        }

        @Override public int size() { return m.size(); }
        @Override public boolean isEmpty() { return m.isEmpty(); }
        @Override public boolean containsKey(Object k) { return m.containsKey(k); }
        @Override public boolean containsValue(Object v) { return m.containsValue(v); }
        @Override public V get(Object k) { return m.get(k); }
        @Override public V put(K k, V v) { throw new UnsupportedOperationException(); }
        @Override public V remove(Object k) { throw new UnsupportedOperationException(); }
        @Override public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public Set<K> keySet() { return Collections.unmodifiableSet(m.keySet()); }
        @Override public Collection<V> values() { return Collections.unmodifiableCollection(m.values()); }
        @Override public Set<Entry<K, V>> entrySet() { return Collections.unmodifiableSet((Set)m.entrySet()); }
        @Override public boolean equals(Object o) { return m.equals(o); }
        @Override public int hashCode() { return m.hashCode(); }
        @Override public V getOrDefault(Object k, V defaultValue) {
            V v = m.get(k);
            return (v != null || m.containsKey(k)) ? v : defaultValue;
        }
        @Override public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) {
            V v = m.get(k);
            if (v != null) return v;
            V newV = f.apply((K) k);
            if (newV != null) m.put((K) k, newV);
            return newV;
        }
    }

    private static class UnmodifiableList<T> implements List<T> {
        private final List<T> list;

        UnmodifiableList(List<T> list) {
            this.list = list;
        }

        @Override public int size() { return list.size(); }
        @Override public boolean isEmpty() { return list.isEmpty(); }
        @Override public boolean contains(Object o) { return list.contains(o); }
        @Override public Object[] toArray() { return list.toArray(); }
        @Override public <T2> T2[] toArray(T2[] a) { return list.toArray(a); }
        @Override public Iterator<T> iterator() { return unmodifiableIterator(list.iterator()); }
        @Override public ListIterator<T> listIterator() { return unmodifiableListIterator(list.listIterator()); }
        @Override public ListIterator<T> listIterator(int index) { return unmodifiableListIterator(list.listIterator(index)); }
        @Override public boolean add(T e) { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return list.containsAll(c); }
        @Override public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        @Override public boolean addAll(int index, Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public boolean equals(Object o) { return list.equals(o); }
        @Override public int hashCode() { return list.hashCode(); }
        @Override public T get(int index) { return list.get(index); }
        @Override public T set(int index, T element) { throw new UnsupportedOperationException(); }
        @Override public void add(int index, T element) { throw new UnsupportedOperationException(); }
        @Override public T remove(int index) { throw new UnsupportedOperationException(); }
        @Override public int indexOf(Object o) { return list.indexOf(o); }
        @Override public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
        @Override public List<T> subList(int from, int to) { return unmodifiableList(list.subList(from, to)); }
        @Override public void replaceAll(java.util.function.UnaryOperator<T> operator) { throw new UnsupportedOperationException(); }
        @Override public void sort(Comparator<? super T> c) { throw new UnsupportedOperationException(); }
        @Override public Spliterator<T> spliterator() { return list.spliterator(); }
    }

    private static class UnmodifiableCollection<T> implements Collection<T> {
        private final Collection<T> c;

        UnmodifiableCollection(Collection<T> c) {
            this.c = c;
        }

        @Override public int size() { return c.size(); }
        @Override public boolean isEmpty() { return c.isEmpty(); }
        @Override public boolean contains(Object o) { return c.contains(o); }
        @Override public Object[] toArray() { return c.toArray(); }
        @Override public <T2> T2[] toArray(T2[] a) { return c.toArray(a); }
        @Override public Iterator<T> iterator() { return unmodifiableIterator(c.iterator()); }
        @Override public boolean add(T e) { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(Collection<?> c) { return this.c.containsAll(c); }
        @Override public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public Spliterator<T> spliterator() { return c.spliterator(); }
    }

    private static class UnmodifiableIterator<T> implements Iterator<T> {
        private final Iterator<T> i;

        UnmodifiableIterator(Iterator<T> i) {
            this.i = i;
        }

        @Override public boolean hasNext() { return i.hasNext(); }
        @Override public T next() { return i.next(); }
        @Override public void remove() { throw new UnsupportedOperationException(); }
    }

    private static class UnmodifiableListIterator<T> implements ListIterator<T> {
        private final ListIterator<T> i;

        UnmodifiableListIterator(ListIterator<T> i) {
            this.i = i;
        }

        @Override public boolean hasNext() { return i.hasNext(); }
        @Override public T next() { return i.next(); }
        @Override public boolean hasPrevious() { return i.hasPrevious(); }
        @Override public T previous() { return i.previous(); }
        @Override public int nextIndex() { return i.nextIndex(); }
        @Override public int previousIndex() { return i.previousIndex(); }
        @Override public void remove() { throw new UnsupportedOperationException(); }
        @Override public void set(T e) { throw new UnsupportedOperationException(); }
        @Override public void add(T e) { throw new UnsupportedOperationException(); }
    }

    private static class UnmodifiableEnumeration<T> implements Enumeration<T> {
        private final Enumeration<T> e;

        UnmodifiableEnumeration(Enumeration<T> e) {
            this.e = e;
        }

        @Override public boolean hasMoreElements() { return e.hasMoreElements(); }
        @Override public T nextElement() { return e.nextElement(); }
    }

    public static <T> Set<T> singleton(T o) {
        return new SingletonSet<>(o);
    }

    private static class SingletonSet<T> extends AbstractSet<T> {
        private final T element;

        SingletonSet(T element) {
            this.element = element;
        }

        @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
                private boolean hasNext = true;
                @Override public boolean hasNext() { return hasNext; }
                @Override public T next() {
                    if (!hasNext) throw new NoSuchElementException();
                    hasNext = false;
                    return element;
                }
            };
        }

        @Override public int size() { return 1; }

        @Override public boolean contains(Object o) {
            return o == null ? element == null : o.equals(element);
        }
    }

    public static <T> List<T> emptyList() {
        return (List<T>) EmptyList.INSTANCE;
    }

    public static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) EmptyMap.INSTANCE;
    }

    public static <T> Set<T> emptySet() {
        return (Set<T>) EmptySet.INSTANCE;
    }

    private static class EmptyList<T> extends AbstractList<T> {
        static final EmptyList INSTANCE = new EmptyList<>();

        @Override public int size() { return 0; }
        @Override public T get(int index) { throw new IndexOutOfBoundsException(); }
        @Override public Iterator<T> iterator() { return emptyIterator(); }
        @Override public ListIterator<T> listIterator() { return emptyListIterator(); }
        @Override public boolean addAll(int index, Collection<? extends T> c) { return false; }
        @Override public Spliterator<T> spliterator() { return Spliterators.emptySpliterator(); }
    }

    private static class EmptySet<T> extends AbstractSet<T> {
        static final EmptySet INSTANCE = new EmptySet<>();

        @Override public int size() { return 0; }
        @Override public Iterator<T> iterator() { return emptyIterator(); }
    }

    private static class EmptyMap<K, V> extends AbstractMap<K, V> {
        static final EmptyMap INSTANCE = new EmptyMap<>();

        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public V get(Object key) { return null; }
        @Override public boolean containsKey(Object key) { return false; }
        @Override public Set<Entry<K, V>> entrySet() { return emptySet(); }
        @Override public Set<K> keySet() { return emptySet(); }
        @Override public Collection<V> values() { return emptyCollection(); }
        @Override public V getOrDefault(Object key, V defaultValue) { return defaultValue; }
        @Override public V computeIfAbsent(Object k, java.util.function.Function<? super K, ? extends V> f) { return f.apply((K) k); }
    }

    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.INSTANCE;
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return EmptyListIterator.INSTANCE;
    }

    private static class EmptyCollection<T> extends AbstractCollection<T> {
        static final EmptyCollection INSTANCE = new EmptyCollection<>();

        @Override public int size() { return 0; }
        @Override public Iterator<T> iterator() { return emptyIterator(); }
    }

    public static <T> Collection<T> emptyCollection() {
        return EmptyCollection.INSTANCE;
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        return new Enumeration<T>() {
            @Override public boolean hasMoreElements() { return false; }
            @Override public T nextElement() { throw new NoSuchElementException(); }
        };
    }

    public static <T> Enumeration<T> enumeration(Collection<T> c) {
        final Iterator<T> i = c.iterator();
        return new Enumeration<T>() {
            @Override public boolean hasMoreElements() { return i.hasNext(); }
            @Override public T nextElement() { return i.next(); }
        };
    }

    private static class EmptyIterator<T> implements Iterator<T> {
        static final EmptyIterator INSTANCE = new EmptyIterator<>();

        @Override public boolean hasNext() { return false; }
        @Override public T next() { throw new NoSuchElementException(); }
    }

    private static class EmptyListIterator<T> implements ListIterator<T> {
        static final EmptyListIterator INSTANCE = new EmptyListIterator<>();

        @Override public boolean hasNext() { return false; }
        @Override public T next() { throw new NoSuchElementException(); }
        @Override public boolean hasPrevious() { return false; }
        @Override public T previous() { throw new NoSuchElementException(); }
        @Override public int nextIndex() { return 0; }
        @Override public int previousIndex() { return -1; }
        @Override public void remove() { throw new UnsupportedOperationException(); }
        @Override public void set(T e) { throw new UnsupportedOperationException(); }
        @Override public void add(T e) { throw new UnsupportedOperationException(); }
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        Object[] a = list.toArray();
        Arrays.sort(a);
        int i = 0;
        for (T o : list) {
            // Preserve elements - this is a stub
        }
    }

    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        // Stub implementation
    }

    public static <T> void shuffle(List<T> list) {
        // Stub implementation
    }

    public static <T> void shuffle(List<T> list, Random rnd) {
        // Stub implementation
    }

    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            int cmp = midVal.compareTo(key);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public static <T> void reverse(List<?> list) {
        // Stub
    }

    public static void rotate(List<?> list, int distance) {
        // Stub
    }

    public static <T> void swap(List<?> list, int i, int j) {
        // Stub
    }

    public static <T> int frequency(Collection<?> c, Object o) {
        int count = 0;
        for (Object e : c) {
            if ((o == null && e == null) || (o != null && o.equals(e))) {
                count++;
            }
        }
        return count;
    }

    public static <T> boolean disjoint(Collection<?> c1, Collection<?> c2) {
        // Stub
        return false;
    }

    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll) {
        return min(coll, Comparable::compareTo);
    }

    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        return max(coll, Comparable::compareTo);
    }

    public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        // Stub
        return null;
    }

    public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        // Stub
        return null;
    }

    public static <T> List<T> checkedList(List<T> list, Class<T> type) {
        return list;
    }

    public static <T> Set<T> checkedSet(Set<T> set, Class<T> type) {
        return set;
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> map, Class<K> keyType, Class<V> valueType) {
        return map;
    }

    public static <T> List<T> singletonList(T o) {
        List<T> list = new java.util.ArrayList<>();
        list.add(o);
        return unmodifiableList(list);
    }
}
