package java.util.concurrent.locks;

public interface Condition {
    void await() throws InterruptedException;
    void awaitUninterruptibly();
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    boolean await(long time, java.util.concurrent.TimeUnit unit) throws InterruptedException;
    boolean awaitUntil(java.util.Date deadline) throws InterruptedException;
    void signal();
    void signalAll();
}
