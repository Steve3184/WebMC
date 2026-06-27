package java.lang.invoke;

public final class MethodType {
    private MethodType() {}
    public static MethodType methodType(Class<?> rtype) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, Class<?>... ptypes) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>... ptypes) { return new MethodType(); }
    public static MethodType methodType(Class<?> rtype, java.util.List<Class<?>> ptypes) { return new MethodType(); }
    public static MethodType genericMethodType(int objectArgCount) { return new MethodType(); }
    public static MethodType genericMethodType(int objectArgCount, boolean finalArray) { return new MethodType(); }
    public Class<?> returnType() { return Object.class; }
    public java.util.List<Class<?>> parameterList() { return java.util.List.of(); }
    public Class<?>[] parameterArray() { return new Class<?>[0]; }
    public int parameterCount() { return 0; }
    public Class<?> parameterType(int n) { return Object.class; }
}
