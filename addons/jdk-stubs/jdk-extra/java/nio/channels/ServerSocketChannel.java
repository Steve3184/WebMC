package java.nio.channels;

public abstract class ServerSocketChannel extends java.nio.channels.spi.AbstractSelectableChannel implements NetworkChannel {
    protected ServerSocketChannel(java.nio.channels.spi.SelectorProvider provider) { super(provider); }
    public static ServerSocketChannel open() { throw new UnsupportedOperationException(); }
    @Override public final int validOps() { return SelectionKey.OP_ACCEPT; }
    public abstract ServerSocketChannel bind(java.net.SocketAddress local, int backlog);
    public final ServerSocketChannel bind(java.net.SocketAddress local) { return bind(local, 0); }
    public abstract <T> ServerSocketChannel setOption(java.net.SocketOption<T> name, T value);
    public abstract java.net.ServerSocket socket();
    public abstract SocketChannel accept();
    public abstract java.net.SocketAddress getLocalAddress();
}
