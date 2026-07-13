package sun.nio.ch;

import java.nio.channels.spi.Selector;
import java.nio.channels.spi.SelectorProvider;

public class DefaultSelectorProvider {
    private static SelectorProvider provider;
    
    public static SelectorProvider get() {
        if (provider == null) {
            provider = new SelectorProvider() {
                public java.nio.channels.DatagramChannel openDatagramChannel() { return null; }
                public java.nio.channels.Pipe openPipe() { return null; }
                public Selector openSelector() { return null; }
                public java.nio.channels.ServerSocketChannel openServerSocketChannel() { return null; }
                public java.nio.channels.SocketChannel openSocketChannel() { return null; }
                public java.nio.channels.DatagramChannel openDatagramChannel(java.net.ProtocolFamily family) { return null; }
                public java.nio.channels.SocketChannel openSocketChannel(java.net.ProtocolFamily family) { return null; }
            };
        }
        return provider;
    }
}
