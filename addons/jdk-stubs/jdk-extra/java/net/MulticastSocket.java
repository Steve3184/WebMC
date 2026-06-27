package java.net;

public class MulticastSocket extends DatagramSocket {
    public MulticastSocket() {}
    public MulticastSocket(int port) { super(port); }
    public MulticastSocket(SocketAddress bindaddr) { super(bindaddr); }
    public void setTimeToLive(int ttl) {}
    public int getTimeToLive() { return 0; }
    public void joinGroup(InetAddress mcastaddr) {}
    public void leaveGroup(InetAddress mcastaddr) {}
    public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) {}
    public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) {}
    public void setInterface(InetAddress inf) {}
    public InetAddress getInterface() { return null; }
    public void setNetworkInterface(NetworkInterface netIf) {}
    public NetworkInterface getNetworkInterface() { return null; }
    public void setLoopbackMode(boolean disable) {}
    public boolean getLoopbackMode() { return false; }
    public void send(DatagramPacket p, byte ttl) {}
}
