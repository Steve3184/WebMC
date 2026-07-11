package java.util.function;

public interface BiFunction<T, U, R> {
    R apply(T t, U u);
    default <V> BiFunction<T, U, V> andThen(java.util.function.Function<? super R, ? extends V> after) {
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
