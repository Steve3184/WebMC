package org.apache.logging.log4j;

public final class LogManager {
    private LogManager() {}
    public static Logger getLogger() { return new NopLogger(); }
    public static Logger getLogger(String name) { return new NopLogger(); }
    public static Logger getLogger(Class<?> cls) { return new NopLogger(); }
    public static Logger getRootLogger() { return new NopLogger(); }
    public static String ROOT_LOGGER_NAME = "";

    private static final class NopLogger implements Logger {
        @Override public void trace(String message) {}
        @Override public void debug(String message) {}
        @Override public void info(String message) {}
        @Override public void warn(String message) {}
        @Override public void error(String message) {}
        @Override public void fatal(String message) {}
        @Override public void error(String message, Throwable t) {}
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isInfoEnabled() { return false; }
        @Override public boolean isWarnEnabled() { return false; }
        @Override public boolean isErrorEnabled() { return false; }
        @Override public String getName() { return ""; }
    }
}
