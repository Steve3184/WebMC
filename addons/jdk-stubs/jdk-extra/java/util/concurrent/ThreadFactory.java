package java.util.concurrent;

@FunctionalInterface
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
