package java.net;

public class Socket implements java.io.Closeable {
    public Socket() {}
    public Socket(String host, int port) {}
    public Socket(InetAddress address, int port) {}
    public synchronized void close() {}
    public boolean isClosed() { return false; }
    public boolean isConnected() { return false; }
    public InetAddress getInetAddress() { return null; }
    public int getPort() { return -1; }
    public InetAddress getLocalAddress() { return InetAddress.getLoopbackAddress(); }
    public int getLocalPort() { return -1; }
    public java.io.InputStream getInputStream() { throw new UnsupportedOperationException(); }
    public java.io.OutputStream getOutputStream() { throw new UnsupportedOperationException(); }
    public void connect(SocketAddress endpoint) {}
    public void connect(SocketAddress endpoint, int timeout) {}
    public void bind(SocketAddress bindpoint) {}
    public SocketAddress getLocalSocketAddress() { return null; }
    public SocketAddress getRemoteSocketAddress() { return null; }
    public boolean isInputShutdown() { return false; }
    public boolean isOutputShutdown() { return false; }
    public void shutdownInput() {}
    public void shutdownOutput() {}
    public boolean isBound() { return false; }
    public synchronized int getSoTimeout() { return 0; }
    public synchronized void setSoTimeout(int timeout) {}
    public boolean getKeepAlive() { return false; }
    public void setKeepAlive(boolean on) {}
    public boolean getReuseAddress() { return false; }
    public void setReuseAddress(boolean on) {}
    public int getSendBufferSize() { return 0; }
    public void setSendBufferSize(int size) {}
    public int getReceiveBufferSize() { return 0; }
    public void setReceiveBufferSize(int size) {}
    public int getSoLinger() { return -1; }
    public void setSoLinger(boolean on, int linger) {}
    public boolean getTcpNoDelay() { return false; }
    public void setTcpNoDelay(boolean on) {}
    public int getTrafficClass() { return 0; }
    public void setTrafficClass(int tc) {}
}
