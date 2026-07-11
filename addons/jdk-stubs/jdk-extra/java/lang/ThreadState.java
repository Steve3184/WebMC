package java.lang;

public class ThreadState {
    public static final ThreadState NEW = new ThreadState("NEW");
    public static final ThreadState RUNNABLE = new ThreadState("RUNNABLE");
    public static final ThreadState BLOCKED = new ThreadState("BLOCKED");
    public static final ThreadState WAITING = new ThreadState("WAITING");
    public static final ThreadState TIMED_WAITING = new ThreadState("TIMED_WAITING");
    public static final ThreadState TERMINATED = new ThreadState("TERMINATED");

    private final String name;

    private ThreadState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
