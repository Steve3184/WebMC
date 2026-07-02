package javax.net.ssl;

public class TrustManagerFactory {
    private TrustManagerFactory() {}
    public static TrustManagerFactory getInstance(String algorithm) { return new TrustManagerFactory(); }
    public static String getDefaultAlgorithm() { return "SunX509"; }
    public final String getAlgorithm() { return "SunX509"; }
    public final void init(java.security.KeyStore ks) {}
    public final TrustManager[] getTrustManagers() { return new TrustManager[0]; }
}
