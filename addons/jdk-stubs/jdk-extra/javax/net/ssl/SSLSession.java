package javax.net.ssl;
import java.security.Principal;
public interface SSLSession {
    byte[] getId();
    SSLSessionContext getSessionContext();
    long getCreationTime();
    long getLastAccessedTime();
    void invalidate();
    boolean isValid();
    void putValue(String name, Object value);
    Object getValue(String name);
    void removeValue(String name);
    String[] getValueNames();
    Principal getPeerPrincipal();
    java.security.cert.Certificate[] getPeerCertificates();
    String getProtocol();
    String getCipherSuite();
}
