package java.util.concurrent;

import java.util.Collection;
import java.util.List;

/** ExecutorService — TeaVM 0.13.x lacks this interface even though it has
 *  TExecutor. Provides the methods AbstractExecutorService implements. */
public interface ExecutorService extends Executor {
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit);
    <T> Future<T> submit(Callable<T> task);
    <T> Future<T> submit(Runnable task, T result);
    Future<?> submit(Runnable task);
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks);
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit);
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException;
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException;
}
