package java.security;

public class KeyFactory {
    private final String algorithm;
    protected KeyFactory(KeyFactorySpi keyFacSpi, java.security.Provider provider, String algorithm) { this.algorithm = algorithm; }
    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("KeyFactory not available in browser: " + algorithm);
    }
    public final String getAlgorithm() { return algorithm; }
    public final PublicKey generatePublic(java.security.spec.KeySpec keySpec) { throw new UnsupportedOperationException(); }
    public final PrivateKey generatePrivate(java.security.spec.KeySpec keySpec) { throw new UnsupportedOperationException(); }
    public final <T extends java.security.spec.KeySpec> T getKeySpec(Key key, Class<T> keySpec) { throw new UnsupportedOperationException(); }
    public final Key translateKey(Key key) { throw new UnsupportedOperationException(); }
}
