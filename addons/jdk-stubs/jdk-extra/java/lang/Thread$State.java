package java.lang;

/** Minimal Thread$State — only static fields, no methods.
 *  AsyncMethodFinder SOE was triggered when methods (values/valueOf) were
 *  present, possibly because TeaVM's renamer entangled them with TThread.
 *  With no methods, there's nothing for AsyncMethodFinder to recurse through. */
public class Thread$State {
    public static final Thread$State NEW = new Thread$State();
    public static final Thread$State RUNNABLE = new Thread$State();
    public static final Thread$State BLOCKED = new Thread$State();
    public static final Thread$State WAITING = new Thread$State();
    public static final Thread$State TIMED_WAITING = new Thread$State();
    public static final Thread$State TERMINATED = new Thread$State();
    private Thread$State() {}
}
