package java.util.concurrent;

import java.util.AbstractCollection;

/** Minimal ForkJoinPool — runs tasks inline. */
public class ForkJoinPool extends AbstractExecutorService {
    private static final ForkJoinPool COMMON = new ForkJoinPool();
    public static ForkJoinPool commonPool() { return COMMON; }
    public static int getCommonPoolParallelism() { return 1; }
    public static int getActiveThreadCount() { return 0; }

    public ForkJoinPool() {}
    public ForkJoinPool(int parallelism) {}
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode) {}

    @Override public void execute(Runnable command) { command.run(); }
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) { task.invoke(); return task; }

    public int getParallelism() { return 1; }
    public int getPoolSize() { return 1; }
    public int getRunningThreadCount() { return 0; }
    public boolean hasQueuedSubmissions() { return false; }
    public long getQueuedTaskCount() { return 0; }
    public int getQueuedSubmissionCount() { return 0; }
    public long getStealCount() { return 0; }

    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory = pool -> null;
}
