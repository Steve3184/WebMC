package java.net;

public class Proxy {
    public static final Proxy NO_PROXY = new Proxy(Type.DIRECT, null);
    private final Type type;
    private final SocketAddress sa;
    public Proxy(Type type, SocketAddress sa) { this.type = type; this.sa = sa; }
    public Type type() { return type; }
    public SocketAddress address() { return sa; }
    public enum Type { DIRECT, HTTP, SOCKS }
    @Override public String toString() { return type + " @ " + sa; }
    @Override public boolean equals(Object obj) { return obj == this; }
    @Override public int hashCode() { return type.hashCode(); }
}
