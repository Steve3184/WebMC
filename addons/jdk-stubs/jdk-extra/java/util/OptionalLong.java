package java.util;

import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.LongStream;

public class OptionalLong {
    private static final OptionalLong EMPTY = new OptionalLong();
    private final boolean isPresent;
    private final long value;

    private OptionalLong() {
        this.isPresent = false;
        this.value = 0;
    }

    private OptionalLong(long value) {
        this.isPresent = true;
        this.value = value;
    }

    public static OptionalLong empty() {
        return EMPTY;
    }

    public static OptionalLong of(long value) {
        return new OptionalLong(value);
    }

    public long getAsLong() {
        if (!isPresent) throw new java.util.NoSuchElementException();
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(java.util.function.LongConsumer action) {
        if (isPresent) action.accept(value);
    }

    public void ifPresentOrElse(java.util.function.LongConsumer action, Runnable emptyAction) {
        if (isPresent) action.accept(value);
        else emptyAction.run();
    }

    public OptionalLong filter(java.util.function.LongPredicate predicate) {
        if (!isPresent) return this;
        return predicate.test(value) ? this : empty();
    }

    public OptionalLong map(java.util.function.LongUnaryOperator mapper) {
        if (!isPresent) return empty();
        return of(mapper.applyAsLong(value));
    }

    public OptionalLong flatMap(java.util.function.LongFunction<OptionalLong> mapper) {
        if (!isPresent) return this;
        return mapper.apply(value);
    }

    public long orElse(long other) {
        return isPresent ? value : other;
    }

    public long orElseGet(java.util.function.LongSupplier supplier) {
        return isPresent ? value : supplier.getAsLong();
    }

    public long orElseThrow() {
        if (!isPresent) throw new java.util.NoSuchElementException();
        return value;
    }

    public <X extends Throwable> long orElseThrow(java.util.function.Supplier<X> exceptionSupplier) throws X {
        if (isPresent) return value;
        else throw exceptionSupplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OptionalLong)) return false;
        OptionalLong other = (OptionalLong) obj;
        if (isPresent != other.isPresent) return false;
        return isPresent && value == other.value;
    }

    @Override
    public int hashCode() {
        return isPresent ? Long.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        return isPresent ? "OptionalLong[" + value + "]" : "OptionalLong.empty";
    }
}
