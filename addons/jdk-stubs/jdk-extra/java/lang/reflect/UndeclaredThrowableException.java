package java.lang.reflect;

public class UndeclaredThrowableException extends RuntimeException {
    private final Throwable undeclaredThrowable;
    public UndeclaredThrowableException(Throwable t) { this.undeclaredThrowable = t; }
    public UndeclaredThrowableException(Throwable t, String s) { super(s); this.undeclaredThrowable = t; }
    public Throwable getUndeclaredThrowable() { return undeclaredThrowable; }
}
