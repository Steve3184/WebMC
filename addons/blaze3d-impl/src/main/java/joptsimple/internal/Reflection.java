package joptsimple.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import joptsimple.ValueConverter;

/**
 * Browser-side replacement for joptsimple.internal.Reflection.
 *
 * The original calls {@code clazz.getMethod("valueOf", String.class)} and
 * {@code clazz.getConstructor(String.class)} via reflection. TeaVM's
 * reflection emulation can't see those for unannotated JDK classes (File,
 * String, Integer, etc.) and throws {@code "X is not a value type"}.
 *
 * We replace findConverter with a hand-rolled lookup table covering the
 * types MC's Main actually uses, then fall through to a String passthrough
 * for anything else.
 */
public final class Reflection {
    private Reflection() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <V> ValueConverter<V> findConverter(Class<V> clazz) {
        if (clazz == String.class) {
            return (ValueConverter<V>) new StringPassthroughConverter();
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (ValueConverter<V>) new IntegerConverter();
        }
        if (clazz == Long.class || clazz == long.class) {
            return (ValueConverter<V>) new LongConverter();
        }
        if (clazz == Double.class || clazz == double.class) {
            return (ValueConverter<V>) new DoubleConverter();
        }
        if (clazz == Float.class || clazz == float.class) {
            return (ValueConverter<V>) new FloatConverter();
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (ValueConverter<V>) new BooleanConverter();
        }
        if (clazz == java.io.File.class) {
            return (ValueConverter<V>) new FileConverter();
        }
        // Fallback: treat as String. Lossy but won't blow up for niche options.
        return (ValueConverter<V>) new StringPassthroughConverter();
    }

    public static <T> T instantiate(Constructor<T> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception ex) {
            throw reflectionException(ex);
        }
    }

    public static Object invoke(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Exception ex) {
            throw reflectionException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <V> V convertWith(ValueConverter<V> converter, String raw) {
        return converter == null ? (V) raw : converter.convert(raw);
    }

    private static RuntimeException reflectionException(Exception ex) {
        if (ex instanceof IllegalArgumentException) return new ReflectionException(ex);
        if (ex instanceof InvocationTargetException) return new ReflectionException(ex.getCause());
        if (ex instanceof RuntimeException) return (RuntimeException) ex;
        return new ReflectionException(ex);
    }

    private static abstract class BaseConverter<V> implements ValueConverter<V> {
        @Override public String valuePattern() { return null; }
    }

    private static final class StringPassthroughConverter extends BaseConverter<String> {
        @Override public String convert(String value) { return value; }
        @Override public Class<? extends String> valueType() { return String.class; }
    }

    private static final class IntegerConverter extends BaseConverter<Integer> {
        @Override public Integer convert(String value) { return Integer.valueOf(value); }
        @Override public Class<? extends Integer> valueType() { return Integer.class; }
    }

    private static final class LongConverter extends BaseConverter<Long> {
        @Override public Long convert(String value) { return Long.valueOf(value); }
        @Override public Class<? extends Long> valueType() { return Long.class; }
    }

    private static final class DoubleConverter extends BaseConverter<Double> {
        @Override public Double convert(String value) { return Double.valueOf(value); }
        @Override public Class<? extends Double> valueType() { return Double.class; }
    }

    private static final class FloatConverter extends BaseConverter<Float> {
        @Override public Float convert(String value) { return Float.valueOf(value); }
        @Override public Class<? extends Float> valueType() { return Float.class; }
    }

    private static final class BooleanConverter extends BaseConverter<Boolean> {
        @Override public Boolean convert(String value) { return Boolean.valueOf(value); }
        @Override public Class<? extends Boolean> valueType() { return Boolean.class; }
    }

    private static final class FileConverter extends BaseConverter<java.io.File> {
        @Override public java.io.File convert(String value) { return new java.io.File(value); }
        @Override public Class<? extends java.io.File> valueType() { return java.io.File.class; }
    }
}
