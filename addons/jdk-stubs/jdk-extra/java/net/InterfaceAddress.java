package java.net;

public class InterfaceAddress {
    InterfaceAddress() {}
    public InetAddress getAddress() { return InetAddress.getLoopbackAddress(); }
    public InetAddress getBroadcast() { return null; }
    public short getNetworkPrefixLength() { return 8; }
}
