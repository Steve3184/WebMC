package java.security.cert;

public abstract class Certificate implements java.io.Serializable {
    private final String type;
    protected Certificate(String type) { this.type = type; }
    public final String getType() { return type; }
    public abstract byte[] getEncoded();
    public abstract void verify(java.security.PublicKey key);
    public abstract void verify(java.security.PublicKey key, String sigProvider);
    public abstract String toString();
    public abstract java.security.PublicKey getPublicKey();

    public void checkValidity() throws CertificateException {}
    public void checkValidity(java.util.Date date) throws CertificateException {}
}
