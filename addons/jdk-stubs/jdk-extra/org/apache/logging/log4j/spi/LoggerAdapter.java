package org.apache.logging.log4j.spi;
import org.apache.logging.log4j.Logger;
public class LoggerAdapter<T> {
    protected final T logger;
    public LoggerAdapter() { this.logger = null; }
    public LoggerAdapter(T logger) { this.logger = logger; }
    public T getLogger() { return logger; }
}
