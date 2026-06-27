package io.netty.util.concurrent;

public interface EventExecutor {
    boolean inEventLoop();
    boolean inEventLoop(Thread thread);
    void execute(Runnable command);
    boolean isTerminated();
    boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit);
    boolean isShuttingDown();
    boolean isShutdown();
    void shutdown();
}
