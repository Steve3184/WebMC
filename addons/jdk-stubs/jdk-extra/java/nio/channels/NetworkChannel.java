package java.nio.channels;

public interface NetworkChannel extends java.io.Closeable {
    NetworkChannel bind(java.net.SocketAddress local);
    java.net.SocketAddress getLocalAddress();
    <T> NetworkChannel setOption(java.net.SocketOption<T> name, T value);
    <T> T getOption(java.net.SocketOption<T> name);
    java.util.Set<java.net.SocketOption<?>> supportedOptions();
}
