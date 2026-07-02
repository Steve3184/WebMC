package org.osgi.framework;

public interface Bundle {
    int UNINSTALLED = 1;
    int INSTALLED = 2;
    int RESOLVED = 4;
    int STARTING = 8;
    int STOPPING = 16;
    int ACTIVE = 32;
    int getState();
    void start();
    void stop();
    String getSymbolicName();
    long getBundleId();
    BundleContext getBundleContext();
    java.util.Dictionary<String, String> getHeaders();
    java.net.URL getResource(String name);
    Class<?> loadClass(String name);
}
