package javax.net.ssl;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
public abstract class SSLSocket extends Socket {
    public abstract String[] getSupportedCipherSuites();
    public abstract String[] getEnabledCipherSuites();
    public abstract void setEnabledCipherSuites(String[] suites);
    public abstract String[] getSupportedProtocols();
    public abstract String[] getEnabledProtocols();
    public abstract void setEnabledProtocols(String[] protocols);
    public abstract SSLSession getSession();
    public abstract void addHandshakeCompletedListener(HandshakeCompletedListener listener);
    public abstract void removeHandshakeCompletedListener(HandshakeCompletedListener listener);
    public abstract void startHandshake() throws java.io.IOException;
    public abstract void setUseClientMode(boolean mode);
    public abstract boolean getUseClientMode();
    public abstract void setNeedClientAuth(boolean need);
    public abstract boolean getNeedClientAuth();
    public abstract void setWantClientAuth(boolean want);
    public abstract boolean getWantClientAuth();
    public abstract void setEnableSessionCreation(boolean flag);
    public abstract boolean getEnableSessionCreation();
}
