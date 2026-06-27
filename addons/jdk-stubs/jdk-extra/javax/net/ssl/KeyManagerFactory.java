package javax.net.ssl;

public class KeyManagerFactory {
    private KeyManagerFactory() {}
    public static KeyManagerFactory getInstance(String algorithm) { return new KeyManagerFactory(); }
    public static String getDefaultAlgorithm() { return "SunX509"; }
    public final String getAlgorithm() { return "SunX509"; }
    public final void init(java.security.KeyStore ks, char[] password) {}
    public final KeyManager[] getKeyManagers() { return new KeyManager[0]; }
}
