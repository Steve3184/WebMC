package java.lang.invoke;

/** MethodHandle stub — TeaVM 0.13.x has no real MethodHandle support.
 *  Java code compiled with `invokedynamic` may reference these; we provide
 *  empty bodies so reachability analysis succeeds. Runtime calls throw. */
public abstract class MethodHandle {
    public Object invoke(Object... args) { throw new UnsupportedOperationException(); }
    public Object invokeExact(Object... args) { throw new UnsupportedOperationException(); }
    // Signature-polymorphic stubs for various return types — JDK uses JVM
    // magic to dispatch invokeExact()/invoke() with arbitrary signatures;
    // TeaVM's static analysis treats each variant as a separate method.
    public int invokeExact() { return 0; }
    public void invoke(long arg) {}
    public Object invokeWithArguments(Object... args) { throw new UnsupportedOperationException(); }
    public Object invokeWithArguments(java.util.List<?> args) { throw new UnsupportedOperationException(); }
    public MethodType type() { return null; }
    public MethodHandle bindTo(Object x) { return this; }
    public MethodHandle asType(MethodType newType) { return this; }
    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) { return this; }
    public MethodHandle asCollector(Class<?> arrayType, int arrayLength) { return this; }
    public MethodHandle asVarargsCollector(Class<?> arrayType) { return this; }
    public boolean isVarargsCollector() { return false; }
    public MethodHandle asFixedArity() { return this; }
}
