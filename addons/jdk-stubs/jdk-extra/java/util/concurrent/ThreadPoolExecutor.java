package java.util.concurrent;

import java.util.Collection;
import java.util.List;

public class ThreadPoolExecutor extends AbstractExecutorService {
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {}
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {}
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {}
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {}

    @Override public void execute(Runnable command) { command.run(); }
    public int getCorePoolSize() { return 1; }
    public void setCorePoolSize(int corePoolSize) {}
    public int getMaximumPoolSize() { return 1; }
    public void setMaximumPoolSize(int maximumPoolSize) {}
    public BlockingQueue<Runnable> getQueue() { return null; }
    public RejectedExecutionHandler getRejectedExecutionHandler() { return null; }
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {}
    public ThreadFactory getThreadFactory() { return null; }
    public void setThreadFactory(ThreadFactory threadFactory) {}
    public int getActiveCount() { return 0; }
    public int getPoolSize() { return 0; }
    public long getCompletedTaskCount() { return 0; }
    public long getTaskCount() { return 0; }
    public boolean prestartCoreThread() { return false; }
    public int prestartAllCoreThreads() { return 0; }
    public void allowCoreThreadTimeOut(boolean value) {}
    public boolean allowsCoreThreadTimeOut() { return false; }
    public boolean remove(Runnable task) { return false; }
    public void purge() {}

    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        public AbortPolicy() {}
        @Override public void rejectedExecution(Runnable r, ThreadPoolExecutor e) { throw new RejectedExecutionException(); }
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        public CallerRunsPolicy() {}
        @Override public void rejectedExecution(Runnable r, ThreadPoolExecutor e) { r.run(); }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler {
        public DiscardPolicy() {}
        @Override public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {}
    }

    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        public DiscardOldestPolicy() {}
        @Override public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {}
    }
}
