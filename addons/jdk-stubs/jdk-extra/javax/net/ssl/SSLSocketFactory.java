package javax.net.ssl;

import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;

public abstract class SSLSocketFactory {
    protected SSLSocketFactory() {}

    public static SSLSocketFactory getDefault() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            return context.getSocketFactory();
        } catch (Exception e) {
            return null;
        }
    }

    public abstract String[] getDefaultCipherSuites();
    public abstract String[] getSupportedCipherSuites();

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return null;
    }

    public Socket createSocket(String host, int port) throws IOException {
        return null;
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return null;
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return null;
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return null;
    }
}
