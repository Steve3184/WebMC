package java.util.concurrent;

public class Semaphore implements java.io.Serializable {
    private int permits;
    public Semaphore(int permits) { this.permits = permits; }
    public Semaphore(int permits, boolean fair) { this.permits = permits; }
    public void acquire() {}
    public void acquireUninterruptibly() {}
    public boolean tryAcquire() { if (permits > 0) { permits--; return true; } return false; }
    public boolean tryAcquire(long timeout, TimeUnit unit) { return tryAcquire(); }
    public void release() { permits++; }
    public void acquire(int n) { permits -= n; }
    public boolean tryAcquire(int n) { if (permits >= n) { permits -= n; return true; } return false; }
    public boolean tryAcquire(int n, long timeout, TimeUnit unit) { return tryAcquire(n); }
    public void release(int n) { permits += n; }
    public int availablePermits() { return permits; }
    public int drainPermits() { int r = permits; permits = 0; return r; }
    public boolean isFair() { return false; }
    public boolean hasQueuedThreads() { return false; }
    public int getQueueLength() { return 0; }
}
