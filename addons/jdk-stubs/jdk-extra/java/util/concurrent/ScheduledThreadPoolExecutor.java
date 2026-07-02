package java.util.concurrent;

public class ScheduledThreadPoolExecutor extends Executors.InlineScheduledExecutor {
    public ScheduledThreadPoolExecutor(int corePoolSize) {}
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {}

    public int getCorePoolSize() { return 1; }
    public void setCorePoolSize(int corePoolSize) {}
    public boolean getRemoveOnCancelPolicy() { return false; }
    public void setRemoveOnCancelPolicy(boolean value) {}
    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() { return false; }
    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {}
    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() { return true; }
    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {}
}
