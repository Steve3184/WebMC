package org.apache.log4j;

public class Level extends Priority {
    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);
    public static final Level FATAL = new Level("FATAL", 50000);
    public static final Level ERROR = new Level("ERROR", 40000);
    public static final Level WARN = new Level("WARN", 30000);
    public static final Level INFO = new Level("INFO", 20000);
    public static final Level DEBUG = new Level("DEBUG", 10000);
    public static final Level TRACE = new Level("TRACE", 5000);
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);
    protected Level(String name, int value) { super(name, value); }
    public static Level toLevel(String s) { return INFO; }
    public static Level toLevel(int v) { return INFO; }
    public static Level toLevel(String s, Level defaultLevel) { return defaultLevel; }
}
