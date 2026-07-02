package java.nio.channels;

public abstract class SocketChannel extends java.nio.channels.spi.AbstractSelectableChannel implements ByteChannel, NetworkChannel, GatheringByteChannel, ScatteringByteChannel {
    protected SocketChannel(java.nio.channels.spi.SelectorProvider provider) { super(provider); }
    public static SocketChannel open() { throw new UnsupportedOperationException(); }
    public static SocketChannel open(java.net.SocketAddress remote) { throw new UnsupportedOperationException(); }
    @Override public final int validOps() { return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT; }
    public abstract SocketChannel shutdownInput();
    public abstract SocketChannel shutdownOutput();
    public abstract java.net.Socket socket();
    public abstract boolean isConnected();
    public abstract boolean isConnectionPending();
    public abstract boolean connect(java.net.SocketAddress remote);
    public abstract boolean finishConnect();
    public abstract java.net.SocketAddress getRemoteAddress();
}
