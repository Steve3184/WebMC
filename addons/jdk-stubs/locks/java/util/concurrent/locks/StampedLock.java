package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/** No-op StampedLock — JS single-threaded means all ops succeed instantly. */
public class StampedLock {
    private long stamp = 1L;
    public StampedLock() {}
    public long writeLock() { return ++stamp; }
    public long readLock() { return ++stamp; }
    public long tryWriteLock() { return ++stamp; }
    public long tryWriteLock(long time, TimeUnit unit) { return ++stamp; }
    public long tryReadLock() { return ++stamp; }
    public long tryReadLock(long time, TimeUnit unit) { return ++stamp; }
    public long tryOptimisticRead() { return stamp; }
    public boolean validate(long stamp) { return true; }
    public void unlockWrite(long stamp) {}
    public void unlockRead(long stamp) {}
    public void unlock(long stamp) {}
    public long tryConvertToWriteLock(long stamp) { return stamp; }
    public long tryConvertToReadLock(long stamp) { return stamp; }
    public long tryConvertToOptimisticRead(long stamp) { return stamp; }
    public boolean tryUnlockWrite() { return true; }
    public boolean tryUnlockRead() { return true; }
    public boolean isWriteLocked() { return false; }
    public boolean isReadLocked() { return false; }
    public int getReadLockCount() { return 0; }
    public Lock asReadLock() { return new ReentrantLock(); }
    public Lock asWriteLock() { return new ReentrantLock(); }
    public ReadWriteLock asReadWriteLock() { return new ReentrantReadWriteLock(); }
}
