package java.util;

/**
 * Stub for java.util.Spliterators — TeaVM has its own Spliterators
 * (TSpliterators) but apparently misses the AbstractSpliterator inner class.
 * Only the AbstractSpliterator nested class is referenced from MC, so this
 * stub provides exactly that. The outer Spliterators class is "shadowed" but
 * since real callers use TeaVM's TSpliterators (renamed), our outer class
 * never executes — it's just a container for AbstractSpliterator.
 */
public final class Spliterators {
    private Spliterators() {}

    public static <T> Spliterator<T> emptySpliterator() {
        return new AbstractSpliterator<T>(0L, Spliterator.SIZED) {
            @Override public boolean tryAdvance(java.util.function.Consumer<? super T> action) { return false; }
        };
    }

    public static <T> java.util.Iterator<T> iterator(Spliterator<? extends T> spliterator) {
        return new java.util.Iterator<T>() {
            T next; boolean has;
            { advance(); }
            void advance() { has = spliterator.tryAdvance(t -> { next = t; }); }
            @Override public boolean hasNext() { return has; }
            @Override public T next() { T v = next; advance(); return v; }
        };
    }

    public static java.util.PrimitiveIterator.OfInt iterator(Spliterator.OfInt spliterator) {
        return new java.util.PrimitiveIterator.OfInt() {
            int next; boolean has;
            { advance(); }
            void advance() { has = spliterator.tryAdvance((java.util.function.IntConsumer) t -> { next = t; }); }
            @Override public boolean hasNext() { return has; }
            @Override public int nextInt() { int v = next; advance(); return v; }
        };
    }

    public static java.util.PrimitiveIterator.OfLong iterator(Spliterator.OfLong spliterator) {
        return new java.util.PrimitiveIterator.OfLong() {
            long next; boolean has;
            { advance(); }
            void advance() { has = spliterator.tryAdvance((java.util.function.LongConsumer) t -> { next = t; }); }
            @Override public boolean hasNext() { return has; }
            @Override public long nextLong() { long v = next; advance(); return v; }
        };
    }

    public static abstract class AbstractSpliterator<T> implements Spliterator<T> {
        private final long est;
        private final int characteristics;
        protected AbstractSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = additionalCharacteristics;
        }
        @Override public Spliterator<T> trySplit() { return null; }
        @Override public long estimateSize() { return est; }
        @Override public int characteristics() { return characteristics; }
    }

    public static abstract class AbstractIntSpliterator implements Spliterator.OfInt {
        private final long est;
        private final int characteristics;
        protected AbstractIntSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = additionalCharacteristics;
        }
        @Override public Spliterator.OfInt trySplit() { return null; }
        @Override public long estimateSize() { return est; }
        @Override public int characteristics() { return characteristics; }
    }

    public static abstract class AbstractLongSpliterator implements Spliterator.OfLong {
        private final long est;
        private final int characteristics;
        protected AbstractLongSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = additionalCharacteristics;
        }
        @Override public Spliterator.OfLong trySplit() { return null; }
        @Override public long estimateSize() { return est; }
        @Override public int characteristics() { return characteristics; }
    }
}
