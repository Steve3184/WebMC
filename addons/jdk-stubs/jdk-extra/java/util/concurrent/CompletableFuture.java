package java.util.concurrent;

/**
 * Minimal CompletableFuture stub — synchronous in JS single-threaded runtime.
 * Stage methods execute immediately; async variants ignore Executor.
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    private T value;
    private Throwable error;
    private boolean done;
    private boolean cancelled;

    public CompletableFuture() {}

    public static <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> f = new CompletableFuture<>();
        f.complete(value);
        return f;
    }
    public static <U> CompletableFuture<U> failedFuture(Throwable t) {
        CompletableFuture<U> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }
    public static CompletableFuture<Void> runAsync(Runnable r) {
        try { r.run(); return completedFuture(null); }
        catch (Throwable t) { return failedFuture(t); }
    }
    public static CompletableFuture<Void> runAsync(Runnable r, java.util.concurrent.Executor exec) { return runAsync(r); }
    public static <U> CompletableFuture<U> supplyAsync(java.util.function.Supplier<U> s) {
        try { return completedFuture(s.get()); }
        catch (Throwable t) { return failedFuture(t); }
    }
    public static <U> CompletableFuture<U> supplyAsync(java.util.function.Supplier<U> s, java.util.concurrent.Executor exec) { return supplyAsync(s); }
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        for (CompletableFuture<?> cf : cfs) if (cf.done) return (CompletableFuture<Object>) cf;
        return cfs.length > 0 ? (CompletableFuture<Object>) cfs[0] : new CompletableFuture<>();
    }
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) { return completedFuture(null); }

    public boolean complete(T value) {
        if (done) return false;
        this.value = value; this.done = true; return true;
    }
    public boolean completeExceptionally(Throwable ex) {
        if (done) return false;
        this.error = ex; this.done = true; return true;
    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        if (done) return false;
        cancelled = true; done = true; return true;
    }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public boolean isDone() { return done; }
    public boolean isCompletedExceptionally() { return done && error != null; }
    @Override public T get() throws ExecutionException {
        if (error != null) throw new ExecutionException(error);
        return value;
    }
    @Override public T get(long timeout, TimeUnit unit) throws ExecutionException { return get(); }
    public T join() {
        if (error != null) throw new java.util.concurrent.CompletionException(error);
        return value;
    }
    public T getNow(T valueIfAbsent) { return done ? value : valueIfAbsent; }

    @Override public <U> CompletableFuture<U> thenApply(java.util.function.Function<? super T, ? extends U> fn) {
        if (error != null) return failedFuture(error);
        try { return completedFuture(fn.apply(value)); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public <U> CompletableFuture<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn) { return thenApply(fn); }
    @Override public <U> CompletableFuture<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn, java.util.concurrent.Executor e) { return thenApply(fn); }
    @Override public CompletableFuture<Void> thenAccept(java.util.function.Consumer<? super T> action) {
        if (error != null) return failedFuture(error);
        try { action.accept(value); return completedFuture(null); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public CompletableFuture<Void> thenAcceptAsync(java.util.function.Consumer<? super T> a) { return thenAccept(a); }
    @Override public CompletableFuture<Void> thenAcceptAsync(java.util.function.Consumer<? super T> a, java.util.concurrent.Executor e) { return thenAccept(a); }
    @Override public CompletableFuture<Void> thenRun(Runnable r) {
        if (error != null) return failedFuture(error);
        try { r.run(); return completedFuture(null); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public CompletableFuture<Void> thenRunAsync(Runnable r) { return thenRun(r); }
    @Override public CompletableFuture<Void> thenRunAsync(Runnable r, java.util.concurrent.Executor e) { return thenRun(r); }

    @Override public <U> CompletableFuture<U> thenCompose(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn) {
        if (error != null) return failedFuture(error);
        try { return (CompletableFuture<U>) fn.apply(value); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public <U> CompletableFuture<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn) { return thenCompose(fn); }
    @Override public <U> CompletableFuture<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn, java.util.concurrent.Executor e) { return thenCompose(fn); }

    @Override public CompletableFuture<T> exceptionally(java.util.function.Function<Throwable, ? extends T> fn) {
        if (error == null) return this;
        try { return completedFuture(fn.apply(error)); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public CompletableFuture<T> whenComplete(java.util.function.BiConsumer<? super T, ? super Throwable> action) {
        try { action.accept(value, error); } catch (Throwable t) { /* ignore */ }
        return this;
    }
    @Override public CompletableFuture<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> a) { return whenComplete(a); }
    @Override public CompletableFuture<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> a, java.util.concurrent.Executor e) { return whenComplete(a); }

    @Override public <U> CompletableFuture<U> handle(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn) {
        try { return completedFuture(fn.apply(value, error)); }
        catch (Throwable t) { return failedFuture(t); }
    }
    @Override public <U> CompletableFuture<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn) { return handle(fn); }
    @Override public <U> CompletableFuture<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn, java.util.concurrent.Executor e) { return handle(fn); }

    public CompletableFuture<T> toCompletableFuture() { return this; }

    public <U,V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, java.util.function.BiFunction<? super T, ? super U, ? extends V> fn) {
        if (error != null) return failedFuture(error);
        try {
            CompletableFuture<? extends U> o = other.toCompletableFuture();
            return completedFuture(fn.apply(value, o.value));
        } catch (Throwable t) { return failedFuture(t); }
    }
    public <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, java.util.function.BiFunction<? super T, ? super U, ? extends V> fn) { return thenCombine(other, fn); }
    public <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, java.util.function.BiFunction<? super T, ? super U, ? extends V> fn, java.util.concurrent.Executor e) { return thenCombine(other, fn); }
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn) {
        if (done) return thenApply(fn);
        return ((CompletableFuture<T>) other.toCompletableFuture()).thenApply(fn);
    }
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn) { return applyToEither(other, fn); }
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn, java.util.concurrent.Executor e) { return applyToEither(other, fn); }

    public CompletableFuture<T> exceptionallyAsync(java.util.function.Function<Throwable, ? extends T> fn) { return exceptionally(fn); }
    public CompletableFuture<T> exceptionallyAsync(java.util.function.Function<Throwable, ? extends T> fn, java.util.concurrent.Executor e) { return exceptionally(fn); }
    public CompletableFuture<T> exceptionallyCompose(java.util.function.Function<Throwable, ? extends CompletionStage<T>> fn) {
        if (error == null) return this;
        try { return (CompletableFuture<T>) fn.apply(error); }
        catch (Throwable t) { return failedFuture(t); }
    }
    public CompletableFuture<T> exceptionallyComposeAsync(java.util.function.Function<Throwable, ? extends CompletionStage<T>> fn) { return exceptionallyCompose(fn); }
    public CompletableFuture<T> exceptionallyComposeAsync(java.util.function.Function<Throwable, ? extends CompletionStage<T>> fn, java.util.concurrent.Executor e) { return exceptionallyCompose(fn); }
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) { return this; }
    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) { if (!done) complete(value); return this; }
    public CompletableFuture<T> copy() { CompletableFuture<T> r = new CompletableFuture<>(); r.value = value; r.error = error; r.done = done; r.cancelled = cancelled; return r; }
    public <U> CompletableFuture<U> newIncompleteFuture() { return new CompletableFuture<>(); }
    public java.util.concurrent.Executor defaultExecutor() { return Runnable::run; }
    public CompletableFuture<T> minimalCompletionStage() { return this; }
    public boolean isCompletedExceptionallyOrCancelled() { return error != null || cancelled; }
}
