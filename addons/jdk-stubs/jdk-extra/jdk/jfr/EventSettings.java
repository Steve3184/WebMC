package jdk.jfr;

public abstract class EventSettings {
    protected EventSettings() {}
    public abstract EventSettings withPeriod(java.time.Duration duration);
    public abstract EventSettings withThreshold(java.time.Duration duration);
    public abstract EventSettings withStackTrace();
    public abstract EventSettings withoutStackTrace();
    public abstract EventSettings with(String name, String value);
    public abstract java.util.Map<String, String> toMap();
}
