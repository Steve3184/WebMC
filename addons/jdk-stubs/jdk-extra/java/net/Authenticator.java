package java.net;

public abstract class Authenticator {
    public Authenticator() {}
    public static synchronized void setDefault(Authenticator a) {}
    public static Authenticator getDefault() { return null; }
    public static java.net.PasswordAuthentication requestPasswordAuthentication(java.net.InetAddress addr, int port, String protocol, String prompt, String scheme) { return null; }
    public static java.net.PasswordAuthentication requestPasswordAuthentication(String host, java.net.InetAddress addr, int port, String protocol, String prompt, String scheme) { return null; }
    public static java.net.PasswordAuthentication requestPasswordAuthentication(String host, java.net.InetAddress addr, int port, String protocol, String prompt, String scheme, java.net.URL url, RequestorType reqType) { return null; }
    protected java.net.PasswordAuthentication getPasswordAuthentication() { return null; }
    public enum RequestorType { PROXY, SERVER }
}
