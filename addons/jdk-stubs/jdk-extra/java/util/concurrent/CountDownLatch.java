package java.util.concurrent;

/** No-op CountDownLatch — JS is single-threaded so countDown decrements
 *  the count but await() never blocks. */
public class CountDownLatch {
    private long count;
    public CountDownLatch(int count) { this.count = count; }
    public void await() {}
    public boolean await(long timeout, TimeUnit unit) { return true; }
    public void countDown() { if (count > 0) count--; }
    public long getCount() { return count; }
    @Override public String toString() { return "CountDownLatch(" + count + ")"; }
}
