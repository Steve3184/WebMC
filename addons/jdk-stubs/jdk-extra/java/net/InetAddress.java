package java.net;

public class InetAddress implements java.io.Serializable {
    private final String host;
    InetAddress() { host = "localhost"; }
    InetAddress(String host) { this.host = host; }
    public static InetAddress getByName(String host) { return new InetAddress(host); }
    public static InetAddress getByAddress(byte[] addr) { return new InetAddress("0.0.0.0"); }
    public static InetAddress getByAddress(String host, byte[] addr) { return new InetAddress(host); }
    public static InetAddress getLocalHost() { return new InetAddress("localhost"); }
    public static InetAddress getLoopbackAddress() { return new InetAddress("127.0.0.1"); }
    public static InetAddress[] getAllByName(String host) { return new InetAddress[] { new InetAddress(host) }; }
    public String getHostAddress() { return "0.0.0.0"; }
    public String getHostName() { return host; }
    public String getCanonicalHostName() { return host; }
    public byte[] getAddress() { return new byte[] {0, 0, 0, 0}; }
    public boolean isAnyLocalAddress() { return false; }
    public boolean isLoopbackAddress() { return "localhost".equals(host) || "127.0.0.1".equals(host); }
    public boolean isLinkLocalAddress() { return false; }
    public boolean isSiteLocalAddress() { return false; }
    public boolean isMulticastAddress() { return false; }
    public boolean isReachable(int timeout) { return false; }
    public boolean isReachable(NetworkInterface netif, int ttl, int timeout) { return false; }
    @Override public int hashCode() { return host.hashCode(); }
    @Override public boolean equals(Object obj) { return obj instanceof InetAddress && ((InetAddress) obj).host.equals(host); }
    @Override public String toString() { return host; }
}
