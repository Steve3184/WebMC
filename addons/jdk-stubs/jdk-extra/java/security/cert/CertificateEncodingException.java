package java.security.cert;
public class CertificateEncodingException extends CertificateException {
    private static final long serialVersionUID = 4114292895081854642L;
    public CertificateEncodingException() { super(); }
    public CertificateEncodingException(String message) { super(message); }
    public CertificateEncodingException(String message, Throwable cause) { super(message, cause); }
}
