package java.net;

public class NoRouteToHostException extends SocketException {
    public NoRouteToHostException() {}
    public NoRouteToHostException(String msg) { super(msg); }
}
