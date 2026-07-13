package javax.net.ssl;

import java.security.Principal;
import java.security.cert.Certificate;

public interface SSLSession {
    byte[] getId();
    SessionContext getSessionContext();
    long getCreationTime();
    long getLastAccessedTime();
    void invalidate();
    boolean isValid();
    void putValue(String name, Object value);
    Object getValue(String name);
    void removeValue(String name);
    String[] getValueNames();
    Certificate[] getPeerCertificates();
    Certificate[] getLocalCertificates();
    Principal getPeerPrincipal();
    Principal getLocalPrincipal();
    String getCipherSuite();
    String getProtocol();
    String getPeerHost();
    int getPeerPort();
    int getPacketBufferSize();
    int getApplicationBufferSize();
}
