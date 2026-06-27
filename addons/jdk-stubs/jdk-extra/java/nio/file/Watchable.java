package java.nio.file;

public interface Watchable {
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers);
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events);
}
