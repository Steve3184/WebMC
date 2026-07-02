package java.nio.channels;

public abstract class Selector implements java.io.Closeable {
    protected Selector() {}
    public static Selector open() { throw new UnsupportedOperationException(); }
    public abstract boolean isOpen();
    public abstract java.nio.channels.spi.SelectorProvider provider();
    public abstract java.util.Set<SelectionKey> keys();
    public abstract java.util.Set<SelectionKey> selectedKeys();
    public abstract int selectNow();
    public abstract int select(long timeout);
    public abstract int select();
    public abstract Selector wakeup();
    @Override public abstract void close();

    public interface SelectionKey {}
}
