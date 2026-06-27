package java.nio.channels.spi;

import java.nio.channels.Selector;

public abstract class AbstractSelector extends Selector {
    private final SelectorProvider provider;
    protected AbstractSelector(SelectorProvider provider) { this.provider = provider; }
    @Override public final SelectorProvider provider() { return provider; }
    protected abstract void implCloseSelector();
    @Override public final void close() { implCloseSelector(); }
    @Override public abstract boolean isOpen();
}
