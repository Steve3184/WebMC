package org.osgi.framework.wiring;

public interface BundleRevision {
    String getSymbolicName();
    org.osgi.framework.Bundle getBundle();
    java.util.List<?> getDeclaredCapabilities(String namespace);
    java.util.List<?> getDeclaredRequirements(String namespace);
    int getTypes();
    BundleWiring getWiring();
}
