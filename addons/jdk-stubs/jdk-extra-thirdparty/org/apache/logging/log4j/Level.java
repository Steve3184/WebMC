package org.apache.logging.log4j;

public class Level {
    public static final Level OFF = new Level("OFF", 0);
    public static final Level FATAL = new Level("FATAL", 100);
    public static final Level ERROR = new Level("ERROR", 200);
    public static final Level WARN = new Level("WARN", 300);
    public static final Level INFO = new Level("INFO", 400);
    public static final Level DEBUG = new Level("DEBUG", 500);
    public static final Level TRACE = new Level("TRACE", 600);
    public static final Level ALL = new Level("ALL", Integer.MAX_VALUE);
    private final String name;
    private final int intLevel;
    private Level(String name, int intLevel) { this.name = name; this.intLevel = intLevel; }
    public String name() { return name; }
    public int intLevel() { return intLevel; }
    public boolean isLessSpecificThan(Level other) { return intLevel >= other.intLevel; }
    public boolean isMoreSpecificThan(Level other) { return intLevel < other.intLevel; }
    public static Level getLevel(String name) { return INFO; }
    public static Level toLevel(String name) { return INFO; }
    public static Level toLevel(String name, Level defaultLevel) { return defaultLevel; }
    @Override public String toString() { return name; }
}
