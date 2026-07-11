package java.lang;

public class Thread {
    private String name;
    private int priority;
    private boolean daemon;
    private Runnable target;
    private ClassLoader contextClassLoader;
    private static int threadInitNum;
    private static synchronized int nextThreadNum() {
        return threadInitNum++;
    }

    public Thread() {
        this(null, null, "Thread-" + nextThreadNum());
    }

    public Thread(Runnable target) {
        this(null, target, "Thread-" + nextThreadNum());
    }

    public Thread(ThreadGroup group, Runnable target) {
        this(group, target, "Thread-" + nextThreadNum());
    }

    public Thread(String name) {
        this(null, null, name);
    }

    public Thread(ThreadGroup group, String name) {
        this(group, null, name);
    }

    public Thread(Runnable target, String name) {
        this(null, target, name);
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        this.group = group;
        this.target = target;
        this.name = name;
        this.stackSize = stackSize;
        if (group == null) {
            this.group = Thread.currentThread().getThreadGroup();
        }
    }

    private ThreadGroup group;
    private long stackSize;

    public void start() {
    }

    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public void interrupt() {
    }

    public static boolean interrupted() {
        return false;
    }

    public boolean isInterrupted() {
        return false;
    }

    public final void join() throws InterruptedException {
    }

    public final void join(long millis) throws InterruptedException {
    }

    public final void join(long millis, int nanos) throws InterruptedException {
    }

    public static void sleep(long millis) throws InterruptedException {
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
    }

    public static void yield() {
    }

    public void setDaemon(boolean on) {
        this.daemon = on;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int newPriority) {
        this.priority = newPriority;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public static int activeCount() {
        return 0;
    }

    public static int enumerate(Thread[] tarray) {
        return 0;
    }

    public static boolean holdsLock(Object obj) {
        return false;
    }

    public static Thread currentThread() {
        return new Thread();
    }

    public long getId() {
        return 0;
    }

    public ThreadState getState() {
        return ThreadState.NEW;
    }

    public void checkAccess() {
    }

    public int countStackFrames() {
        return 0;
    }

    public void setContextClassLoader(ClassLoader cl) {
        this.contextClassLoader = cl;
    }

    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread t, Throwable e);
    }
}
