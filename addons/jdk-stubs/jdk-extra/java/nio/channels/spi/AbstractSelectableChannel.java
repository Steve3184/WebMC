package java.nio.channels.spi;

public abstract class AbstractSelectableChannel extends java.nio.channels.SelectableChannel {
    private final SelectorProvider provider;
    protected AbstractSelectableChannel(SelectorProvider provider) { this.provider = provider; }
    @Override public final SelectorProvider provider() { return provider; }
    @Override public final boolean isRegistered() { return false; }
    @Override public final java.nio.channels.SelectionKey keyFor(java.nio.channels.Selector sel) { return null; }
    @Override public final java.nio.channels.SelectionKey register(java.nio.channels.Selector sel, int ops, Object att) { throw new UnsupportedOperationException(); }
    @Override public final java.nio.channels.SelectionKey register(java.nio.channels.Selector sel, int ops) { return register(sel, ops, null); }
    @Override public final boolean isBlocking() { return true; }
    @Override public final Object blockingLock() { return this; }
    @Override public final java.nio.channels.SelectableChannel configureBlocking(boolean block) { return this; }
    public final void close() { implCloseSelectableChannel(); }
    protected abstract void implCloseSelectableChannel();
    protected abstract void implConfigureBlocking(boolean block);
}
