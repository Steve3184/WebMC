package java.lang;

public class ExceptionInInitializerError extends LinkageError {
    private final Throwable exception;
    public ExceptionInInitializerError() { this.exception = null; }
    public ExceptionInInitializerError(Throwable thrown) { this.exception = thrown; }
    public ExceptionInInitializerError(String s) { super(s); this.exception = null; }
    public Throwable getException() { return exception; }
    @Override public Throwable getCause() { return exception; }
}
