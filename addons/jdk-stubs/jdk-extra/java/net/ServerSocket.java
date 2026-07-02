package java.net;

public class ServerSocket implements java.io.Closeable {
    public ServerSocket() {}
    public ServerSocket(int port) {}
    public ServerSocket(int port, int backlog) {}
    public ServerSocket(int port, int backlog, InetAddress bindAddr) {}
    public java.net.Socket accept() { throw new UnsupportedOperationException(); }
    public synchronized void close() {}
    public boolean isClosed() { return false; }
    public void bind(SocketAddress endpoint) {}
    public void bind(SocketAddress endpoint, int backlog) {}
    public InetAddress getInetAddress() { return null; }
    public int getLocalPort() { return -1; }
    public SocketAddress getLocalSocketAddress() { return null; }
    public boolean isBound() { return false; }
    public synchronized void setSoTimeout(int timeout) {}
    public synchronized int getSoTimeout() { return 0; }
    public synchronized void setReuseAddress(boolean on) {}
    public synchronized boolean getReuseAddress() { return false; }
    public synchronized void setReceiveBufferSize(int size) {}
    public synchronized int getReceiveBufferSize() { return 0; }
}
