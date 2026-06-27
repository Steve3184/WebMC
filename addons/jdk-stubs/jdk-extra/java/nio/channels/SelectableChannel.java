package java.nio.channels;

public abstract class SelectableChannel implements java.io.Closeable {
    protected SelectableChannel() {}
    public abstract java.nio.channels.spi.SelectorProvider provider();
    public abstract int validOps();
    public abstract boolean isRegistered();
    public abstract SelectionKey keyFor(Selector sel);
    public abstract SelectionKey register(Selector sel, int ops);
    public abstract SelectionKey register(Selector sel, int ops, Object att);
    public abstract SelectableChannel configureBlocking(boolean block);
    public abstract boolean isBlocking();
    public abstract Object blockingLock();
    public final boolean isOpen() { return true; }
}
