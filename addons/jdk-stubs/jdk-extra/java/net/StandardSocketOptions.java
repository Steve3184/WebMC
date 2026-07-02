package java.net;

public final class StandardSocketOptions {
    private StandardSocketOptions() {}
    public static final SocketOption<Boolean> SO_BROADCAST = new SimpleOption<>("SO_BROADCAST", Boolean.class);
    public static final SocketOption<Boolean> SO_KEEPALIVE = new SimpleOption<>("SO_KEEPALIVE", Boolean.class);
    public static final SocketOption<Integer> SO_SNDBUF = new SimpleOption<>("SO_SNDBUF", Integer.class);
    public static final SocketOption<Integer> SO_RCVBUF = new SimpleOption<>("SO_RCVBUF", Integer.class);
    public static final SocketOption<Boolean> SO_REUSEADDR = new SimpleOption<>("SO_REUSEADDR", Boolean.class);
    public static final SocketOption<Boolean> SO_REUSEPORT = new SimpleOption<>("SO_REUSEPORT", Boolean.class);
    public static final SocketOption<Integer> SO_LINGER = new SimpleOption<>("SO_LINGER", Integer.class);
    public static final SocketOption<Integer> IP_TOS = new SimpleOption<>("IP_TOS", Integer.class);
    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF = new SimpleOption<>("IP_MULTICAST_IF", NetworkInterface.class);
    public static final SocketOption<Integer> IP_MULTICAST_TTL = new SimpleOption<>("IP_MULTICAST_TTL", Integer.class);
    public static final SocketOption<Boolean> IP_MULTICAST_LOOP = new SimpleOption<>("IP_MULTICAST_LOOP", Boolean.class);
    public static final SocketOption<Boolean> TCP_NODELAY = new SimpleOption<>("TCP_NODELAY", Boolean.class);

    private static final class SimpleOption<T> implements SocketOption<T> {
        private final String name;
        private final Class<T> type;
        SimpleOption(String name, Class<T> type) { this.name = name; this.type = type; }
        @Override public String name() { return name; }
        @Override public Class<T> type() { return type; }
        @Override public String toString() { return name; }
    }
}
