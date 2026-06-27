package javax.crypto.spec;

public class IvParameterSpec implements java.security.spec.AlgorithmParameterSpec {
    private final byte[] iv;
    public IvParameterSpec(byte[] iv) { this.iv = iv == null ? null : iv.clone(); }
    public IvParameterSpec(byte[] iv, int offset, int len) {
        this.iv = new byte[len];
        if (iv != null) System.arraycopy(iv, offset, this.iv, 0, len);
    }
    public byte[] getIV() { return iv == null ? null : iv.clone(); }
}
