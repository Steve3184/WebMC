package java.util.concurrent;

public class CompletionException extends RuntimeException {
    public CompletionException() { super(); }
    public CompletionException(String message) { super(message); }
    public CompletionException(String message, Throwable cause) { super(message, cause); }
    public CompletionException(Throwable cause) { super(cause); }
}
