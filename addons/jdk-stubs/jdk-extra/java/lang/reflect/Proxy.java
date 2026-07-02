package java.lang.reflect;

public class Proxy implements java.io.Serializable {
    protected InvocationHandler h;
    protected Proxy(InvocationHandler h) { this.h = h; }

    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces) {
        throw new UnsupportedOperationException("Proxy not supported in browser");
    }
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) {
        throw new UnsupportedOperationException("Proxy not supported in browser");
    }
    public static boolean isProxyClass(Class<?> cl) { return false; }
    public static InvocationHandler getInvocationHandler(Object proxy) {
        if (proxy instanceof Proxy) return ((Proxy) proxy).h;
        throw new IllegalArgumentException("not a proxy");
    }
}
