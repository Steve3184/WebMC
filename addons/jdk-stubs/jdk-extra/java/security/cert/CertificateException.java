package java.security.cert;

public class CertificateException extends Exception {
    public CertificateException() {}
    public CertificateException(String message) { super(message); }
    public CertificateException(String message, Throwable cause) { super(message, cause); }
    public CertificateException(Throwable cause) { super(cause); }
}
