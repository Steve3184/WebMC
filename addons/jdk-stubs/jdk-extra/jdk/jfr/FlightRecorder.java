package jdk.jfr;

public final class FlightRecorder {
    private FlightRecorder() {}
    public static FlightRecorder getFlightRecorder() { return new FlightRecorder(); }
    public java.util.List<EventType> getEventTypes() { return java.util.Collections.emptyList(); }
    public java.util.List<Recording> getRecordings() { return java.util.Collections.emptyList(); }
    public void register(Class<? extends Event> eventClass) {}
    public void unregister(Class<? extends Event> eventClass) {}
    public static boolean isInitialized() { return false; }
    public static boolean isAvailable() { return false; }
    public static Recording takeSnapshot() { return new Recording(); }
    public static void addPeriodicEvent(Class<? extends Event> eventClass, Runnable hook) {}
    public static boolean removePeriodicEvent(Runnable hook) { return false; }
    public static void addListener(FlightRecorderListener changeListener) {}
    public static boolean removeListener(FlightRecorderListener changeListener) { return false; }
}
