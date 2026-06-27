package java.net;

public final class DatagramPacket {
    private final byte[] buf;
    private int length;
    private InetAddress address;
    private int port;
    public DatagramPacket(byte[] buf, int length) { this.buf = buf; this.length = length; }
    public DatagramPacket(byte[] buf, int offset, int length) { this.buf = buf; this.length = length; }
    public DatagramPacket(byte[] buf, int length, InetAddress address, int port) { this(buf, length); this.address = address; this.port = port; }
    public DatagramPacket(byte[] buf, int offset, int length, SocketAddress address) { this(buf, offset, length); }
    public synchronized InetAddress getAddress() { return address; }
    public synchronized int getPort() { return port; }
    public synchronized byte[] getData() { return buf; }
    public synchronized int getOffset() { return 0; }
    public synchronized int getLength() { return length; }
    public synchronized void setAddress(InetAddress addr) { this.address = addr; }
    public synchronized void setPort(int port) { this.port = port; }
    public synchronized void setData(byte[] buf) {}
    public synchronized void setLength(int length) { this.length = length; }
    public synchronized SocketAddress getSocketAddress() { return null; }
    public synchronized void setSocketAddress(SocketAddress address) {}
}
