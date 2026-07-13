package java.nio.channels.spi;

import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public abstract class SelectorProvider {
    private static SelectorProvider provider;
    
    public static SelectorProvider provider() {
        if (provider == null) {
            provider = new sun.nio.ch.DefaultSelectorProvider().get();
        }
        return provider;
    }
    
    public abstract DatagramChannel openDatagramChannel();
    public abstract Pipe openPipe();
    public abstract Selector openSelector();
    public abstract ServerSocketChannel openServerSocketChannel();
    public abstract SocketChannel openSocketChannel();
    
    public DatagramChannel openDatagramChannel(ProtocolFamily family) {
        return openDatagramChannel();
    }
    
    public SocketChannel openSocketChannel(ProtocolFamily family) {
        return openSocketChannel();
    }
}
