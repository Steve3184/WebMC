package java.lang.invoke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MethodType {
    private MethodType() {}
    public static MethodType methodType(Class<?> rtype) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, Class<?>... ptypes) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>... ptypes) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, List<Class<?>> ptypes) { return new MethodType(); }
    public static MethodType genericMethodType(int objectArgCount) { return new MethodType(); }
    public static MethodType genericMethodType(int objectArgCount, boolean finalArray) { return new MethodType(); }
    public Class<?> returnType() { return Object.class; }
    public List<Class<?>> parameterList() { return Collections.emptyList(); }
    public Class<?>[] parameterArray() { return new Class<?>[0]; }
    public int parameterCount() { return 0; }
    public Class<?> parameterType(int n) { return Object.class; }
}
