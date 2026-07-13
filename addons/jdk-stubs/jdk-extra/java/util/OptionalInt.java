package java.util;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class OptionalInt {
    private static final OptionalInt EMPTY = new OptionalInt();
    private final boolean isPresent;
    private final int value;

    private OptionalInt() {
        this.isPresent = false;
        this.value = 0;
    }

    private OptionalInt(int value) {
        this.isPresent = true;
        this.value = value;
    }

    public static OptionalInt empty() {
        return EMPTY;
    }

    public static OptionalInt of(int value) {
        return new OptionalInt(value);
    }

    public int getAsInt() {
        if (!isPresent) throw new java.util.NoSuchElementException();
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(java.util.function.IntConsumer action) {
        if (isPresent) action.accept(value);
    }

    public void ifPresentOrElse(java.util.function.IntConsumer action, Runnable emptyAction) {
        if (isPresent) action.accept(value);
        else emptyAction.run();
    }

    public OptionalInt filter(java.util.function.IntPredicate predicate) {
        if (!isPresent) return this;
        return predicate.test(value) ? this : empty();
    }

    public OptionalInt map(java.util.function.IntUnaryOperator mapper) {
        if (!isPresent) return empty();
        return of(mapper.applyAsInt(value));
    }

    public OptionalInt flatMap(java.util.function.IntFunction<OptionalInt> mapper) {
        if (!isPresent) return this;
        return mapper.apply(value);
    }

    public int orElse(int other) {
        return isPresent ? value : other;
    }

    public int orElseGet(java.util.function.IntSupplier supplier) {
        return isPresent ? value : supplier.getAsInt();
    }

    public int orElseThrow() {
        if (!isPresent) throw new java.util.NoSuchElementException();
        return value;
    }

    public <X extends Throwable> int orElseThrow(java.util.function.Supplier<X> exceptionSupplier) throws X {
        if (isPresent) return value;
        else throw exceptionSupplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OptionalInt)) return false;
        OptionalInt other = (OptionalInt) obj;
        if (isPresent != other.isPresent) return false;
        return isPresent && value == other.value;
    }

    @Override
    public int hashCode() {
        return isPresent ? Integer.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        return isPresent ? "OptionalInt[" + value + "]" : "OptionalInt.empty";
    }
}
