package java.util.concurrent;

public class FutureTask<V> implements RunnableFuture<V> {
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    private Object outcome;
    private volatile Thread runner;
    private java.util.ArrayList<java.util.concurrent.Callable<V>> waiters;

    private final Callable<V> callable;

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;
        this.waiters = new java.util.ArrayList<>();
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
        this.waiters = new java.util.ArrayList<>();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state != NEW) {
            return false;
        }
        if (mayInterruptIfRunning) {
            state = INTERRUPTING;
        } else {
            state = CANCELLED;
        }
        return true;
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        while (s <= COMPLETING) {
            Thread.sleep(1);
            s = state;
        }
        return report(s);
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        int s = state;
        while (s <= COMPLETING && nanos > 0) {
            Thread.sleep(Math.min(1, nanos / 1_000_000));
            nanos -= 1_000_000;
            s = state;
        }
        if (s <= COMPLETING) {
            throw new TimeoutException();
        }
        return report(s);
    }

    protected void set(V v) {
        if (state == NEW) {
            outcome = v;
            state = COMPLETING;
            state = NORMAL;
        }
    }

    protected void setException(Throwable t) {
        if (state == NEW) {
            outcome = t;
            state = COMPLETING;
            state = EXCEPTIONAL;
        }
    }

    public void run() {
        if (state != NEW || runner != Thread.currentThread()) {
            return;
        }
        try {
            Callable<V> c = callable;
            if (c != null) {
                state = COMPLETING;
                V result = c.call();
                set(result);
            }
        } catch (Throwable t) {
            setException(t);
        }
    }

    protected void finishCompletion() {
        for (Object w : waiters) {
            if (w instanceof java.util.concurrent.Future) {
                ((java.util.concurrent.Future<?>)w).cancel(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) {
            return (V)x;
        }
        if (s >= CANCELLED) {
            throw new CancellationException();
        }
        throw new ExecutionException((Throwable)x);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("FutureTask{");
        switch (state) {
            case NEW:
                sb.append("PENDING");
                break;
            case COMPLETING:
                sb.append("COMPLETING");
                break;
            case NORMAL:
                sb.append("NORMAL");
                break;
            case EXCEPTIONAL:
                sb.append("EXCEPTIONAL");
                break;
            case CANCELLED:
                sb.append("CANCELLED");
                break;
            case INTERRUPTING:
            case INTERRUPTED:
                sb.append("INTERRUPTED");
                break;
            default:
                sb.append("UNKNOWN(").append(state).append(")");
                break;
        }
        sb.append("}");
        return sb.toString();
    }
}
