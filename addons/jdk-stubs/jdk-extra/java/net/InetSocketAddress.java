package java.net;

public class InetSocketAddress extends SocketAddress {
    private final String hostname;
    private final InetAddress address;
    private final int port;
    public InetSocketAddress(int port) { this("0.0.0.0", null, port); }
    public InetSocketAddress(InetAddress addr, int port) { this(addr == null ? "0.0.0.0" : addr.getHostName(), addr, port); }
    public InetSocketAddress(String hostname, int port) { this(hostname, null, port); }
    private InetSocketAddress(String hostname, InetAddress address, int port) {
        this.hostname = hostname; this.address = address; this.port = port;
    }
    public static InetSocketAddress createUnresolved(String host, int port) { return new InetSocketAddress(host, null, port); }
    public final int getPort() { return port; }
    public final InetAddress getAddress() { return address; }
    public final String getHostName() { return hostname; }
    public final String getHostString() { return hostname; }
    public final boolean isUnresolved() { return address == null; }
    @Override public String toString() { return hostname + ":" + port; }
    @Override public final boolean equals(Object obj) { return obj == this; }
    @Override public final int hashCode() { return (hostname == null ? 0 : hostname.hashCode()) * 31 + port; }
}
