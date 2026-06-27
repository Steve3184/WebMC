package java.util.concurrent;

import java.util.Collection;
import java.util.List;

/** Single-threaded JS no-op AbstractExecutorService. Tasks run inline. */
public abstract class AbstractExecutorService implements ExecutorService {
    @Override public abstract void execute(Runnable command);

    @Override public void shutdown() {}
    @Override public List<Runnable> shutdownNow() { return java.util.Collections.emptyList(); }
    @Override public boolean isShutdown() { return false; }
    @Override public boolean isTerminated() { return false; }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }

    @Override public <T> Future<T> submit(java.util.concurrent.Callable<T> task) {
        try { T v = task.call(); return new InlineFuture<T>(v, null); }
        catch (Throwable t) { return new InlineFuture<T>(null, t); }
    }
    @Override public <T> Future<T> submit(Runnable task, T result) {
        try { task.run(); return new InlineFuture<T>(result, null); }
        catch (Throwable t) { return new InlineFuture<T>(null, t); }
    }
    @Override public Future<?> submit(Runnable task) {
        try { task.run(); return new InlineFuture<Object>(null, null); }
        catch (Throwable t) { return new InlineFuture<Object>(null, t); }
    }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends java.util.concurrent.Callable<T>> tasks) {
        java.util.ArrayList<Future<T>> out = new java.util.ArrayList<>();
        for (java.util.concurrent.Callable<T> t : tasks) out.add(submit(t));
        return out;
    }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAll(tasks);
    }
    @Override public <T> T invokeAny(Collection<? extends java.util.concurrent.Callable<T>> tasks) throws ExecutionException {
        for (java.util.concurrent.Callable<T> t : tasks) {
            try { return t.call(); } catch (Throwable e) { throw new ExecutionException(e); }
        }
        throw new ExecutionException(new IllegalStateException("no tasks"));
    }
    @Override public <T> T invokeAny(Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException {
        return invokeAny(tasks);
    }

    private static final class InlineFuture<T> implements Future<T> {
        private final T value; private final Throwable error;
        InlineFuture(T v, Throwable e) { value = v; error = e; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public T get() throws ExecutionException {
            if (error != null) throw new ExecutionException(error);
            return value;
        }
        @Override public T get(long timeout, TimeUnit unit) throws ExecutionException { return get(); }
    }
}
