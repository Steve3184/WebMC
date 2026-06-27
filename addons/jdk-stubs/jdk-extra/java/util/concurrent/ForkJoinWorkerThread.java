package java.util.concurrent;

public class ForkJoinWorkerThread extends Thread {
    protected ForkJoinWorkerThread(ForkJoinPool pool) {}
    public ForkJoinPool getPool() { return ForkJoinPool.commonPool(); }
    public int getPoolIndex() { return 0; }
}
