package java.security.cert;

public class CertificateParsingException extends CertificateException {
    public CertificateParsingException() {}
    public CertificateParsingException(String message) { super(message); }
    public CertificateParsingException(String message, Throwable cause) { super(message, cause); }
    public CertificateParsingException(Throwable cause) { super(cause); }
}
