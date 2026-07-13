package javax.net.ssl;

import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;

public abstract class SSLServerSocketFactory {
    protected SSLServerSocketFactory() {}

    public static SSLServerSocketFactory getDefault() {
        return null;
    }

    public abstract String[] getDefaultCipherSuites();
    public abstract String[] getSupportedCipherSuites();
}
