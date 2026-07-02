package java.util.concurrent;

/** Minimal CompletionStage marker — real interface is much larger. We only stub the bits CompletableFuture uses. */
public interface CompletionStage<T> {
    <U> CompletionStage<U> thenApply(java.util.function.Function<? super T, ? extends U> fn);
    <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn);
    <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn, java.util.concurrent.Executor executor);
    CompletionStage<Void> thenAccept(java.util.function.Consumer<? super T> action);
    CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> action);
    CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> action, java.util.concurrent.Executor executor);
    CompletionStage<Void> thenRun(Runnable action);
    CompletionStage<Void> thenRunAsync(Runnable action);
    CompletionStage<Void> thenRunAsync(Runnable action, java.util.concurrent.Executor executor);
    <U> CompletionStage<U> thenCompose(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn);
    <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn);
    <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn, java.util.concurrent.Executor executor);
    CompletionStage<T> exceptionally(java.util.function.Function<Throwable, ? extends T> fn);
    CompletionStage<T> whenComplete(java.util.function.BiConsumer<? super T, ? super Throwable> action);
    CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> action);
    CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> action, java.util.concurrent.Executor executor);
    <U> CompletionStage<U> handle(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn);
    <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn);
    <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn, java.util.concurrent.Executor executor);
    CompletableFuture<T> toCompletableFuture();
}
