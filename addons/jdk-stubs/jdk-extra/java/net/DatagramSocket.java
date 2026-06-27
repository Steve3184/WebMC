package java.net;

public class DatagramSocket implements java.io.Closeable {
    public DatagramSocket() {}
    public DatagramSocket(int port) {}
    public DatagramSocket(SocketAddress bindaddr) {}
    public void send(DatagramPacket p) {}
    public synchronized void receive(DatagramPacket p) {}
    public InetAddress getLocalAddress() { return InetAddress.getLoopbackAddress(); }
    public int getLocalPort() { return 0; }
    public SocketAddress getLocalSocketAddress() { return null; }
    public synchronized void close() {}
    public boolean isClosed() { return false; }
    public void connect(InetAddress address, int port) {}
    public void connect(SocketAddress addr) {}
    public void disconnect() {}
    public boolean isConnected() { return false; }
    public InetAddress getInetAddress() { return null; }
    public int getPort() { return -1; }
    public synchronized void setSoTimeout(int timeout) {}
    public synchronized int getSoTimeout() { return 0; }
    public synchronized void setSendBufferSize(int size) {}
    public synchronized int getSendBufferSize() { return 0; }
    public synchronized void setReceiveBufferSize(int size) {}
    public synchronized int getReceiveBufferSize() { return 0; }
    public synchronized void setReuseAddress(boolean on) {}
    public synchronized boolean getReuseAddress() { return false; }
    public synchronized void setBroadcast(boolean on) {}
    public synchronized boolean getBroadcast() { return false; }
    public synchronized void setTrafficClass(int tc) {}
    public synchronized int getTrafficClass() { return 0; }
    public boolean isBound() { return false; }
}
