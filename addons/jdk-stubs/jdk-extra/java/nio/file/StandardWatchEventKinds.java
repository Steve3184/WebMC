package java.nio.file;

public final class StandardWatchEventKinds {
    private StandardWatchEventKinds() {}

    public static final WatchEvent.Kind<Path> ENTRY_CREATE = new StdKind<>("ENTRY_CREATE", Path.class);
    public static final WatchEvent.Kind<Path> ENTRY_DELETE = new StdKind<>("ENTRY_DELETE", Path.class);
    public static final WatchEvent.Kind<Path> ENTRY_MODIFY = new StdKind<>("ENTRY_MODIFY", Path.class);
    public static final WatchEvent.Kind<Object> OVERFLOW = new StdKind<>("OVERFLOW", Object.class);

    private static final class StdKind<T> implements WatchEvent.Kind<T> {
        private final String name;
        private final Class<T> type;
        StdKind(String name, Class<T> type) { this.name = name; this.type = type; }
        @Override public String name() { return name; }
        @Override public Class<T> type() { return type; }
        @Override public String toString() { return name; }
    }
}
