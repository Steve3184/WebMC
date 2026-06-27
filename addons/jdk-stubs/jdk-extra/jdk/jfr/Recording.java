package jdk.jfr;

import java.io.Closeable;
import java.nio.file.Path;

public final class Recording implements Closeable {
    private static long nextId = 1L;
    private final long id = nextId++;
    private Path destination;
    public Recording() {}
    public Recording(Configuration configuration) {}
    public long getId() { return id; }
    public void start() {}
    public boolean stop() { return false; }
    public void close() {}
    public void dump(Path destination) {}
    public Path getDestination() { return destination; }
    public void setDestination(Path destination) { this.destination = destination; }
    public RecordingState getState() { return RecordingState.NEW; }
    public String getName() { return ""; }
    public void setName(String name) {}
    public java.util.Map<String, String> getSettings() { return java.util.Collections.emptyMap(); }
    public void setSettings(java.util.Map<String, String> settings) {}
    public void setDumpOnExit(boolean dumpOnExit) {}
    public boolean getDumpOnExit() { return false; }
    public void setToDisk(boolean toDisk) {}
    public boolean isToDisk() { return false; }
    public EventSettings enable(Class<? extends Event> eventClass) {
        return new EventSettings() {
            @Override public EventSettings withPeriod(java.time.Duration duration) { return this; }
            @Override public EventSettings withThreshold(java.time.Duration duration) { return this; }
            @Override public EventSettings withStackTrace() { return this; }
            @Override public EventSettings withoutStackTrace() { return this; }
            @Override public EventSettings with(String name, String value) { return this; }
            @Override public java.util.Map<String, String> toMap() { return java.util.Map.of(); }
        };
    }
    public EventSettings enable(String name) { return enable((Class<? extends Event>) null); }
    public EventSettings disable(Class<? extends Event> eventClass) { return enable(eventClass); }
    public EventSettings disable(String name) { return enable(name); }
}
