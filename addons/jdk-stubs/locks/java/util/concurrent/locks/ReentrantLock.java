package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/** Single-threaded JS no-op implementation of ReentrantLock. */
public class ReentrantLock implements Lock {
    private int holdCount = 0;
    public ReentrantLock() {}
    public ReentrantLock(boolean fair) {}

    @Override public void lock()                { holdCount++; }
    @Override public void lockInterruptibly()   { holdCount++; }
    @Override public boolean tryLock()          { holdCount++; return true; }
    @Override public boolean tryLock(long t, TimeUnit u) { holdCount++; return true; }
    @Override public void unlock()              { if (holdCount > 0) holdCount--; }
    @Override public Condition newCondition()   { return new NoopCondition(); }

    public boolean isHeldByCurrentThread()      { return holdCount > 0; }
    public boolean isLocked()                   { return holdCount > 0; }
    public int getHoldCount()                   { return holdCount; }
    public boolean isFair()                     { return false; }
    public int getQueueLength()                 { return 0; }
    public boolean hasQueuedThreads()           { return false; }
    public boolean hasQueuedThread(Thread t)    { return false; }

    private static final class NoopCondition implements Condition {
        @Override public void await()                        {}
        @Override public boolean await(long t, TimeUnit u)   { return true; }
        @Override public long awaitNanos(long n)             { return 0L; }
        @Override public boolean awaitUntil(java.util.Date d){ return true; }
        @Override public void awaitUninterruptibly()         {}
        @Override public void signal()                       {}
        @Override public void signalAll()                    {}
    }
}
