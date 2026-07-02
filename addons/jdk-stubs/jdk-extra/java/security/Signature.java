package java.security;

public abstract class Signature extends SignatureSpi {
    private final String algorithm;
    protected Signature(String algorithm) { this.algorithm = algorithm; }
    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("Signature not available in browser: " + algorithm);
    }
    public final String getAlgorithm() { return algorithm; }
    public final void initVerify(PublicKey publicKey) {}
    public final void initSign(PrivateKey privateKey) {}
    public final void update(byte b) {}
    public final void update(byte[] data) {}
    public final void update(byte[] data, int off, int len) {}
    public final void update(java.nio.ByteBuffer data) {}
    public final byte[] sign() { return new byte[0]; }
    public final boolean verify(byte[] signature) { return false; }
    public final boolean verify(byte[] signature, int offset, int length) { return false; }
}
