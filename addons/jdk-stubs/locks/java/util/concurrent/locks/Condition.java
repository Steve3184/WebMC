package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public interface Condition {
    void await() throws InterruptedException;
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    boolean awaitUntil(java.util.Date deadline) throws InterruptedException;
    void awaitUninterruptibly();
    void signal();
    void signalAll();
}
