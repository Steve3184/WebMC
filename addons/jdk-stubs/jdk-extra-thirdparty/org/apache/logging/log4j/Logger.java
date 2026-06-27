package org.apache.logging.log4j;

/** Stub for log4j 2.x Logger interface. */
public interface Logger {
    void trace(String message);
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void fatal(String message);
    void error(String message, Throwable t);
    boolean isDebugEnabled();
    boolean isInfoEnabled();
    boolean isWarnEnabled();
    boolean isErrorEnabled();
    String getName();
}
