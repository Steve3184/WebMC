package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/** Single-threaded JS no-op ReentrantReadWriteLock. */
public class ReentrantReadWriteLock implements ReadWriteLock {
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    public ReentrantReadWriteLock() {}
    public ReentrantReadWriteLock(boolean fair) {}

    // Covariant returns — JDK declares these with the subtype, MC code may
    // call `lock.readLock()` and expect ReentrantReadWriteLock.ReadLock.
    @Override public ReadLock readLock()  { return readLock; }
    @Override public WriteLock writeLock() { return writeLock; }

    public boolean isFair() { return false; }
    public int getReadLockCount() { return 0; }
    public boolean isWriteLocked() { return false; }
    public boolean isWriteLockedByCurrentThread() { return false; }
    public int getWriteHoldCount() { return 0; }
    public int getReadHoldCount() { return 0; }
    public boolean hasQueuedThreads() { return false; }
    public boolean hasQueuedThread(Thread t) { return false; }
    public int getQueueLength() { return 0; }

    public static class ReadLock implements Lock {
        ReadLock() {}
        @Override public void lock() {}
        @Override public void lockInterruptibly() {}
        @Override public boolean tryLock() { return true; }
        @Override public boolean tryLock(long t, TimeUnit u) { return true; }
        @Override public void unlock() {}
        @Override public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WriteLock implements Lock {
        WriteLock() {}
        @Override public void lock() {}
        @Override public void lockInterruptibly() {}
        @Override public boolean tryLock() { return true; }
        @Override public boolean tryLock(long t, TimeUnit u) { return true; }
        @Override public void unlock() {}
        @Override public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
        public boolean isHeldByCurrentThread() { return false; }
        public int getHoldCount() { return 0; }
    }
}
