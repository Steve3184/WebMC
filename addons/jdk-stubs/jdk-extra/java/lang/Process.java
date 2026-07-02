package java.lang;

/** java.lang.Process stub — browser has no subprocesses. */
public abstract class Process {
    public abstract java.io.OutputStream getOutputStream();
    public abstract java.io.InputStream getInputStream();
    public abstract java.io.InputStream getErrorStream();
    public abstract int waitFor() throws InterruptedException;
    public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
    public abstract int exitValue();
    public abstract void destroy();
    public Process destroyForcibly() { destroy(); return this; }
    public boolean isAlive() { return false; }
    public long pid() { return -1L; }
    public java.util.concurrent.CompletableFuture<Process> onExit() {
        return java.util.concurrent.CompletableFuture.completedFuture(this);
    }
    public boolean supportsNormalTermination() { return false; }
}
