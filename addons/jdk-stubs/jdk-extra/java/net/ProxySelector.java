package java.net;
import java.io.IOException;
import java.util.List;
public abstract class ProxySelector {
    public static ProxySelector getDefault() { return null; }
    public static void setDefault(ProxySelector selector) {}
    public abstract List<Proxy> select(URI uri);
    public abstract void connectFailed(URI uri, SocketAddress sa, IOException ioe);
}
