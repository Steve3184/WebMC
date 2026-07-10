package org.apache.logging.log4j.spi;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public abstract class AbstractLoggerAdapter<R> extends LoggerAdapter<Object> {
    private final Map<R, org.apache.logging.log4j.Logger> loggerMap = new ConcurrentHashMap<>();
    protected abstract org.apache.logging.log4j.Logger newLogger(String name, R context);
    public org.apache.logging.log4j.Logger getLogger(String name) { return loggerMap.computeIfAbsent(getContext(), c -> newLogger(name, c)); }
    public org.apache.logging.log4j.Logger getLogger(Class<?> clazz) { return getLogger(clazz.getName()); }
    protected abstract R getContext();
}
