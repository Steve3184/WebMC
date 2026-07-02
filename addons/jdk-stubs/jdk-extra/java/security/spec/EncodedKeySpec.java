package java.security.spec;

public abstract class EncodedKeySpec implements KeySpec {
    private final byte[] encoded;
    public EncodedKeySpec(byte[] encoded) { this.encoded = encoded == null ? null : encoded.clone(); }
    public byte[] getEncoded() { return encoded == null ? null : encoded.clone(); }
    public abstract String getFormat();
}
