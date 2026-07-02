package top.steve3184.webmc.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * TeaVM-safe substitute for {@code TypeToken.getParameterized(...)}.
 *
 * <p>Gson's {@code getParameterized} validates the arg count via
 * {@code rawClass.getTypeParameters()}, but TeaVM 0.13.1 returns {@code null}
 * from that reflection call — the subsequent {@code .length} access blows up
 * with "Cannot read properties of null (reading 'data')". We bypass the
 * validation by building our own {@link ParameterizedType} and passing it to
 * {@code TypeToken.get(Type)}.</p>
 */
public final class WebTypes {

    public static ParameterizedType parameterized(final Class<?> rawType, final Type... typeArguments) {
        final Type[] args = typeArguments.clone();
        return new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return args.clone(); }
            @Override public Type getRawType() { return rawType; }
            @Override public Type getOwnerType() { return null; }
            @Override public String toString() {
                StringBuilder sb = new StringBuilder(rawType.getName()).append('<');
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(", ");
                    Type a = args[i];
                    sb.append(a instanceof Class<?> ? ((Class<?>) a).getName() : a.toString());
                }
                return sb.append('>').toString();
            }
        };
    }

    private WebTypes() {}
}
