package java.lang.management;

public interface RuntimeMXBean {
    java.util.List<String> getInputArguments();
    String getName();
    long getStartTime();
    long getUptime();
    long getPid();
}
