package javax.net.ssl;

public interface SessionContext {
    javax.net.ssl.SSLSession getSession(byte[] sessionId);
    java.util.Enumeration<byte[]> getIds();
}
