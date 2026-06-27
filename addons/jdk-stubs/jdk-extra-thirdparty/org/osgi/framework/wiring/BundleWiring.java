package org.osgi.framework.wiring;

public interface BundleWiring {
    org.osgi.framework.Bundle getBundle();
    boolean isCurrent();
    boolean isInUse();
    java.util.List<?> getCapabilities(String namespace);
    java.util.List<?> getRequirements(String namespace);
    BundleRevision getRevision();
    ClassLoader getClassLoader();
}
