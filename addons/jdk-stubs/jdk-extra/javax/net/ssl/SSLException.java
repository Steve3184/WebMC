package javax.net.ssl;
public class SSLException extends Exception {
    private static final long serialVersionUID = 8444489146601758904L;
    public SSLException(String reason) { super(reason); }
    public SSLException(String message, Throwable cause) { super(message, cause); }
}
