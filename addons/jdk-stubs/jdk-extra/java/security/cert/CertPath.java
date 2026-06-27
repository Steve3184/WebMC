package java.security.cert;

public abstract class CertPath implements java.io.Serializable {
    private final String type;
    protected CertPath(String type) { this.type = type; }
    public String getType() { return type; }
    public abstract java.util.List<? extends Certificate> getCertificates();
    public abstract byte[] getEncoded();
    public abstract byte[] getEncoded(String encoding);
    public abstract java.util.Iterator<String> getEncodings();
}
