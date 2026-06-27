package java.security;

public abstract class Provider extends java.util.Properties {
    private final String name;
    private final double version;
    private final String info;
    protected Provider(String name, double version, String info) {
        this.name = name; this.version = version; this.info = info;
    }
    public String getName() { return name; }
    public double getVersion() { return version; }
    public String getInfo() { return info; }
}
