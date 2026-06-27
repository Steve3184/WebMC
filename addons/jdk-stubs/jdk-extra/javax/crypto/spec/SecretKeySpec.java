package javax.crypto.spec;

public class SecretKeySpec implements javax.crypto.SecretKey, java.security.spec.KeySpec {
    private final byte[] key;
    private final String algorithm;
    public SecretKeySpec(byte[] key, String algorithm) { this.key = key.clone(); this.algorithm = algorithm; }
    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        this.key = new byte[len];
        System.arraycopy(key, offset, this.key, 0, len);
        this.algorithm = algorithm;
    }
    @Override public String getAlgorithm() { return algorithm; }
    @Override public String getFormat() { return "RAW"; }
    @Override public byte[] getEncoded() { return key.clone(); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(key) ^ algorithm.hashCode(); }
    @Override public boolean equals(Object obj) {
        return obj instanceof SecretKeySpec other &&
               java.util.Arrays.equals(key, other.key) &&
               algorithm.equals(other.algorithm);
    }
}
