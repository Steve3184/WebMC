package jdk.jfr;

public final class EventType {
    private EventType() {}
    public static EventType getEventType(Class<? extends Event> eventClass) { return new EventType(); }
    public String getName() { return ""; }
    public String getLabel() { return ""; }
    public boolean isEnabled() { return false; }
    public void setEnabled(boolean enabled) {}
}
