package org.apache.logging.log4j.spi;

public class LoggerAdapter<L> {
    protected L logger;

    public LoggerAdapter() {}

    public LoggerAdapter(L logger) {
        this.logger = logger;
    }

    public L getLogger() {
        return logger;
    }

    public void log(String message) {
    }

    public void log(String message, Throwable t) {
    }

    public void log(int level, String message) {
    }

    public void log(int level, String message, Throwable t) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public boolean isWarnEnabled() {
        return false;
    }
}
