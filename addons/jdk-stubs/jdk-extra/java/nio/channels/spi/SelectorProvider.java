package java.nio.channels.spi;

public abstract class SelectorProvider {
    protected SelectorProvider() {}
    public static SelectorProvider provider() { throw new UnsupportedOperationException(); }
    public abstract java.nio.channels.Selector openSelector();
}
