package javax.net;
import java.net.InetAddress;
import java.net.Socket;
public abstract class SocketFactory {
    public static SocketFactory getDefault() { return new DefaultSocketFactory(); }
    public abstract Socket createSocket();
    public Socket createSocket(InetAddress address, int port) { return null; }
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) { return null; }
    public Socket createSocket(String host, int port) { return null; }
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) { return null; }

    private static class DefaultSocketFactory extends SocketFactory {
        public Socket createSocket() { return null; }
    }
}
