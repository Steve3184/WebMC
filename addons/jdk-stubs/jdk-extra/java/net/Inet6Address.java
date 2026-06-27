package java.net;

public final class Inet6Address extends InetAddress {
    Inet6Address() { super("::1"); }
    public boolean isIPv4CompatibleAddress() { return false; }
    public static Inet6Address getByAddress(String host, byte[] addr, NetworkInterface nif) { return new Inet6Address(); }
    public static Inet6Address getByAddress(String host, byte[] addr, int scope_id) { return new Inet6Address(); }
}
