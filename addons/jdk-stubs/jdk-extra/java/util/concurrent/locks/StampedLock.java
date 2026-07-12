package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public class StampedLock implements java.io.Serializable {
    public StampedLock() {}
    public long writeLock() { return 0; }
    public long readLock() { return 0; }
    public long tryWriteLock() { return 0; }
    public long tryReadLock() { return 0; }
    public void unlockWrite(long stamp) {}
    public void unlockRead(long stamp) {}
    public void unlock(long stamp) {}
    public long tryConvertToWriteLock(long stamp) { return 0; }
    public long tryConvertToReadLock(long stamp) { return 0; }
    public boolean isWriteLocked() { return false; }
}
