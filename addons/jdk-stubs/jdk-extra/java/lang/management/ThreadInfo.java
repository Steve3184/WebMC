package java.lang.management;

public interface ThreadInfo {
    long getThreadId();
    String getThreadName();
    // Returns Object instead of Thread.State to avoid javac conflict with our
    // standalone java.lang.Thread$State stub (same bytecode FQN as JDK's
    // Thread.State inner enum).
    Object getThreadState();
    long getBlockedTime();
    long getBlockedCount();
    long getWaitedTime();
    long getWaitedCount();
    StackTraceElement[] getStackTrace();
    boolean isInNative();
    boolean isSuspended();
    String getLockName();
    long getLockOwnerId();
    String getLockOwnerName();
}
