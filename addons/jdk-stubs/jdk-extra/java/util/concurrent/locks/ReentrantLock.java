package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public class ReentrantLock implements Lock, java.io.Serializable {
    public ReentrantLock() {}
    public ReentrantLock(boolean fair) {}
    public void lock() {}
    public void lockInterruptibly() throws InterruptedException {}
    public boolean tryLock() { return false; }
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException { return false; }
    public void unlock() {}
    public Condition newCondition() { return null; }
    public int getHoldCount() { return 0; }
    public boolean isHeldByCurrentThread() { return false; }
    public boolean isLocked() { return false; }
    public final boolean isFair() { return false; }
    public final Thread getOwner() { return null; }
    public final int getQueuedThreads() { return 0; }
    public final int getWaitingThreads(Condition condition) { return 0; }
}
