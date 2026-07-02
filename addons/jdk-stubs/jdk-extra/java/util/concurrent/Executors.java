package java.util.concurrent;

public class Executors {
    private Executors() {}

    public static ExecutorService newFixedThreadPool(int nThreads) { return new InlineExecutor(); }
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory f) { return new InlineExecutor(); }
    public static ExecutorService newSingleThreadExecutor() { return new InlineExecutor(); }
    public static ExecutorService newSingleThreadExecutor(ThreadFactory f) { return new InlineExecutor(); }
    public static ExecutorService newCachedThreadPool() { return new InlineExecutor(); }
    public static ExecutorService newCachedThreadPool(ThreadFactory f) { return new InlineExecutor(); }
    public static ExecutorService newWorkStealingPool() { return new InlineExecutor(); }
    public static ExecutorService newWorkStealingPool(int parallelism) { return new InlineExecutor(); }
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() { return new InlineScheduledExecutor(); }
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory f) { return new InlineScheduledExecutor(); }
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) { return new InlineScheduledExecutor(); }
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory f) { return new InlineScheduledExecutor(); }

    public static ExecutorService unconfigurableExecutorService(ExecutorService e) { return e; }
    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService e) { return e; }

    public static ThreadFactory defaultThreadFactory() { return Thread::new; }
    public static ThreadFactory privilegedThreadFactory() { return Thread::new; }

    public static <T> Callable<T> callable(Runnable task, T result) {
        return () -> { task.run(); return result; };
    }
    public static Callable<Object> callable(Runnable task) {
        return () -> { task.run(); return null; };
    }

    public static class InlineExecutor extends AbstractExecutorService {
        @Override public void execute(Runnable command) { command.run(); }
    }

    public static class InlineScheduledExecutor extends InlineExecutor implements ScheduledExecutorService {
        @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            command.run();
            return new InlineScheduledFuture<Object>(null);
        }
        @Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            try { return new InlineScheduledFuture<V>(callable.call()); }
            catch (Exception e) { return new InlineScheduledFuture<V>(null); }
        }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            command.run();
            return new InlineScheduledFuture<Object>(null);
        }
        @Override public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            command.run();
            return new InlineScheduledFuture<Object>(null);
        }
    }

    static final class InlineScheduledFuture<V> implements ScheduledFuture<V> {
        private final V v;
        InlineScheduledFuture(V v) { this.v = v; }
        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(java.util.concurrent.Delayed o) { return 0; }
        @Override public boolean cancel(boolean m) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return v; }
        @Override public V get(long t, TimeUnit u) { return v; }
    }
}
