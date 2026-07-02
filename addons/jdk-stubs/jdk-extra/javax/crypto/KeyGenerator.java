package javax.crypto;

public class KeyGenerator {
    private KeyGenerator() {}
    public static KeyGenerator getInstance(String algorithm) throws java.security.NoSuchAlgorithmException {
        throw new java.security.NoSuchAlgorithmException("KeyGenerator not available in browser: " + algorithm);
    }
    public final String getAlgorithm() { return ""; }
    public final void init(int keysize) {}
    public final void init(int keysize, java.security.SecureRandom random) {}
    public final SecretKey generateKey() { return null; }
}
