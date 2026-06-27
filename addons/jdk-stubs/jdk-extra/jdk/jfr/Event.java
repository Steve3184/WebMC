package jdk.jfr;

public class Event {
    public Event() {}
    public void begin() {}
    public void end() {}
    public void commit() {}
    public boolean isEnabled() { return false; }
    public boolean shouldCommit() { return false; }
    public void set(int index, Object value) {}
}
