package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/** Single-threaded JS no-op LockSupport. */
public class LockSupport {
    public static void park() { Thread.yield(); }
    public static void park(Object blocker) { Thread.yield(); }
    public static void parkNanos(long nanos) { Thread.yield(); }
    public static void parkNanos(Object blocker, long nanos) { Thread.yield(); }
    public static void parkUntil(long deadline) { Thread.yield(); }
    public static void parkUntil(Object blocker, long deadline) { Thread.yield(); }
    public static void unpark(Thread t) {}
    public static Object getBlocker(Thread t) { return null; }
}
