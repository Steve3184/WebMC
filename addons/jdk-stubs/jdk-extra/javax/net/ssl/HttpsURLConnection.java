package javax.net.ssl;

public abstract class HttpsURLConnection extends java.net.HttpURLConnection {
    protected HttpsURLConnection(java.net.URL url) { super(url); }
    public abstract String getCipherSuite();
    public abstract java.security.cert.Certificate[] getLocalCertificates();
    public abstract java.security.cert.Certificate[] getServerCertificates();
    public static void setDefaultHostnameVerifier(HostnameVerifier v) {}
    public static HostnameVerifier getDefaultHostnameVerifier() { return null; }
    public void setHostnameVerifier(HostnameVerifier v) {}
    public HostnameVerifier getHostnameVerifier() { return null; }
    public static void setDefaultSSLSocketFactory(SSLSocketFactory f) {}
    public static SSLSocketFactory getDefaultSSLSocketFactory() { return null; }
    public void setSSLSocketFactory(SSLSocketFactory f) {}
    public SSLSocketFactory getSSLSocketFactory() { return null; }
    public interface HostnameVerifier { boolean verify(String hostname, SSLSession session); }
    public interface SSLSession {}
}
