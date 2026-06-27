package java.nio.file;

public interface WatchService extends java.io.Closeable {
    @Override void close();
    WatchKey poll();
    WatchKey poll(long timeout, java.util.concurrent.TimeUnit unit);
    WatchKey take();
}
