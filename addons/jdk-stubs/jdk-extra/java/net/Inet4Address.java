package java.net;

public final class Inet4Address extends InetAddress {
    private static final long serialVersionUID = 3286316764910204507L;
    
    public static Inet4Address loopbackAddress() {
        return new Inet4Address("127.0.0.1");
    }
    
    public Inet4Address(String host) { super(host); }
}
