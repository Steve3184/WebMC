package jdk.jfr;

public final class Configuration {
    private Configuration() {}
    public static Configuration getConfiguration(String name) { return null; }
    public static java.util.List<Configuration> getConfigurations() { return java.util.Collections.emptyList(); }
    public static Configuration create(java.io.Reader reader) { return null; }
    public static Configuration create(java.nio.file.Path path) { return null; }
    public String getName() { return ""; }
    public String getLabel() { return ""; }
    public String getDescription() { return ""; }
    public String getProvider() { return ""; }
    public String getContents() { return ""; }
    public java.util.Map<String, String> getSettings() { return java.util.Collections.emptyMap(); }
}
