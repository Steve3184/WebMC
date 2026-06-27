package org.osgi.framework;

public interface BundleContext {
    Bundle getBundle();
    Bundle getBundle(long id);
    Bundle[] getBundles();
    void addBundleListener(Object listener);
    void removeBundleListener(Object listener);
    String getProperty(String key);
    Object getService(Object reference);
    void ungetService(Object reference);
}
