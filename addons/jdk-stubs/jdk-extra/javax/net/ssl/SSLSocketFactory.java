package javax.net.ssl;
import java.net.InetAddress;
import java.net.Socket;
public abstract class SSLSocketFactory extends javax.net.SocketFactory {
    public static javax.net.SocketFactory getDefault() { return new DefaultSSLSocketFactory(); }
    public abstract Socket createSocket(Socket s, String host, int port, boolean autoClose);
    public Socket createSocket(InetAddress address, int port) { return null; }
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) { return null; }
    public Socket createSocket(String host, int port) { return null; }
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) { return null; }
    public String[] getDefaultCipherSuites() { return new String[0]; }
    public String[] getSupportedCipherSuites() { return new String[0]; }

    private static class DefaultSSLSocketFactory extends SSLSocketFactory {
        public Socket createSocket() { return null; }
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) { return null; }
        public Socket createSocket(InetAddress address, int port) { return null; }
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) { return null; }
        public Socket createSocket(String host, int port) { return null; }
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) { return null; }
    }
}
