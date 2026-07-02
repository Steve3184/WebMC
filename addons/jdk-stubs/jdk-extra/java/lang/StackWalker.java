package java.lang;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** java.lang.StackWalker stub — browser has no real stack walking; returns empty. */
public final class StackWalker {
    private StackWalker() {}

    public static StackWalker getInstance() { return new StackWalker(); }
    public static StackWalker getInstance(Option option) { return new StackWalker(); }
    public static StackWalker getInstance(Set<Option> options) { return new StackWalker(); }
    public static StackWalker getInstance(Set<Option> options, int estimateDepth) { return new StackWalker(); }

    public <T> T walk(Function<? super Stream<StackFrame>, ? extends T> function) {
        return function.apply(Stream.empty());
    }
    public void forEach(java.util.function.Consumer<? super StackFrame> action) {}
    public Class<?> getCallerClass() {
        throw new UnsupportedOperationException("getCallerClass not supported in browser");
    }

    public enum Option {
        RETAIN_CLASS_REFERENCE,
        SHOW_REFLECT_FRAMES,
        SHOW_HIDDEN_FRAMES;
    }

    public interface StackFrame {
        String getClassName();
        String getMethodName();
        Class<?> getDeclaringClass();
        int getByteCodeIndex();
        String getFileName();
        int getLineNumber();
        boolean isNativeMethod();
        StackTraceElement toStackTraceElement();
    }
}
