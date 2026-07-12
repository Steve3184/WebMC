package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLock implements ReadWriteLock {
    public ReentrantReadWriteLock() {}
    public ReentrantReadWriteLock(boolean fair) {}
    public ReadLock readLock() { return null; }
    public WriteLock writeLock() { return null; }
    public int getReadLockCount() { return 0; }
    public boolean isWriteLocked() { return false; }
    public static class ReadLock implements Lock {
        public void lock() {}
        public void lockInterruptibly() throws InterruptedException {}
        public boolean tryLock() { return false; }
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException { return false; }
        public void unlock() {}
        public Condition newCondition() { return null; }
    }
    public static class WriteLock implements Lock {
        public void lock() {}
        public void lockInterruptibly() throws InterruptedException {}
        public boolean tryLock() { return false; }
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException { return false; }
        public void unlock() {}
        public Condition newCondition() { return null; }
    }
}
