package java.util;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Optional<T> {
    private final T value;

    private Optional(T value) {
        this.value = value;
    }

    public static <T> Optional<T> empty() {
        return new Optional<>(null);
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    public T get() {
        if (value == null) throw new java.util.NoSuchElementException();
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public void ifPresent(java.util.function.Consumer<? super T> action) {
        if (value != null) action.accept(value);
    }

    public void ifPresentOrElse(java.util.function.Consumer<? super T> action, Runnable emptyAction) {
        if (value != null) action.accept(value);
        else emptyAction.run();
    }

    public Optional<T> filter(java.util.function.Predicate<? super T> predicate) {
        if (!isPresent()) return this;
        return predicate.test(value) ? this : empty();
    }

    public <U> Optional<U> map(java.util.function.Function<? super T, ? extends U> mapper) {
        if (!isPresent()) return empty();
        return Optional.ofNullable(mapper.apply(value));
    }

    public <U> Optional<U> flatMap(java.util.function.Function<? super T, Optional<U>> mapper) {
        if (!isPresent()) return empty();
        return mapper.apply(value);
    }

    public Optional<T> or(java.util.function.Supplier<? extends Optional<? extends T>> supplier) {
        if (isPresent()) return this;
        @SuppressWarnings("unchecked")
        Optional<T> r = (Optional<T>) supplier.get();
        return r;
    }

    public Stream<T> stream() {
        return isPresent() ? Stream.of(value) : Stream.empty();
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(java.util.function.Supplier<? extends T> supplier) {
        return value != null ? value : supplier.get();
    }

    public T orElseThrow() {
        if (value == null) throw new java.util.NoSuchElementException();
        return value;
    }

    public <X extends Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) return value;
        else throw exceptionSupplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Optional)) return false;
        Optional<?> other = (Optional<?>) obj;
        return java.util.Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return isPresent() ? "Optional[" + value + "]" : "Optional.empty";
    }
}
