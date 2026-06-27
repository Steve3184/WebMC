package java.security;

public abstract class KeyPairGenerator extends KeyPairGeneratorSpi {
    private final String algorithm;
    protected KeyPairGenerator(String algorithm) { this.algorithm = algorithm; }
    public String getAlgorithm() { return algorithm; }
    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("KeyPairGenerator not available in browser: " + algorithm);
    }
    public static KeyPairGenerator getInstance(String algorithm, String provider) throws NoSuchAlgorithmException { return getInstance(algorithm); }
    @Override public abstract void initialize(int keysize);
    public void initialize(int keysize, java.security.SecureRandom random) {}
    @Override public abstract KeyPair generateKeyPair();
    public final KeyPair genKeyPair() { return generateKeyPair(); }
}
