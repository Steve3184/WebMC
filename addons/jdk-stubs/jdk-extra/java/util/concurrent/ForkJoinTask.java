package java.util.concurrent;

public abstract class ForkJoinTask<V> implements Future<V> {
    private V value;
    private Throwable error;
    private boolean done;

    public final V invoke() {
        try { value = exec(); done = true; return value; }
        catch (Throwable t) { error = t; done = true; throw new RuntimeException(t); }
    }
    public final ForkJoinTask<V> fork() { invoke(); return this; }
    public final V join() { invoke(); if (error != null) throw new RuntimeException(error); return value; }

    protected abstract V exec() throws Exception;

    @Override public boolean cancel(boolean m) { return false; }
    @Override public boolean isCancelled() { return false; }
    @Override public boolean isDone() { return done; }
    @Override public V get() throws ExecutionException {
        if (error != null) throw new ExecutionException(error);
        return value;
    }
    @Override public V get(long timeout, TimeUnit unit) throws ExecutionException { return get(); }
    public final V getRawResult() { return value; }
    protected void setRawResult(V v) { value = v; }
}
