package javax.net.ssl;

import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;

public class SSLContext {
    private SSLContext() {}
    public static SSLContext getInstance(String protocol) { return new SSLContext(); }
    public static SSLContext getInstance(String protocol, String provider) { return new SSLContext(); }
    public static SSLContext getDefault() { return new SSLContext(); }
    public static void setDefault(SSLContext context) {}
    public final void init(KeyManager[] km, TrustManager[] tm, java.security.SecureRandom random) {}
    public final SSLSocketFactory getSocketFactory() { return null; }
    public final SSLEngine createSSLEngine() { return null; }
    public final SSLEngine createSSLEngine(String peerHost, int peerPort) { return null; }
    public final SSLSessionContext getServerSessionContext() { return null; }
    public final SSLSessionContext getClientSessionContext() { return null; }
    public final String getProtocol() { return "TLS"; }
    public final java.security.Provider getProvider() { return null; }
    public final SSLServerSocketFactory getServerSocketFactory() { return null; }

    public interface SSLEngine {}
    public interface SSLSessionContext {}
}
