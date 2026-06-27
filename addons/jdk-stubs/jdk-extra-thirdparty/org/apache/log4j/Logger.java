package org.apache.log4j;

public class Logger {
    private final String name;
    private Logger(String name) { this.name = name == null ? "" : name; }
    public static Logger getLogger(String name) { return new Logger(name); }
    public static Logger getLogger(Class<?> cls) { return new Logger(cls == null ? "" : cls.getName()); }
    public static Logger getRootLogger() { return new Logger(""); }
    public String getName() { return name; }
    public void trace(Object message) {}
    public void trace(Object message, Throwable t) {}
    public void debug(Object message) {}
    public void debug(Object message, Throwable t) {}
    public void info(Object message) {}
    public void info(Object message, Throwable t) {}
    public void warn(Object message) {}
    public void warn(Object message, Throwable t) {}
    public void error(Object message) {}
    public void error(Object message, Throwable t) {}
    public void fatal(Object message) {}
    public void fatal(Object message, Throwable t) {}
    public void log(Priority level, Object message) {}
    public void log(Priority level, Object message, Throwable t) {}
    public void log(String fqcn, Priority level, Object message, Throwable t) {}
    public boolean isTraceEnabled() { return false; }
    public boolean isDebugEnabled() { return false; }
    public boolean isInfoEnabled() { return false; }
    public boolean isEnabledFor(Priority level) { return false; }
    public Level getLevel() { return Level.INFO; }
    public void setLevel(Level level) {}
}
