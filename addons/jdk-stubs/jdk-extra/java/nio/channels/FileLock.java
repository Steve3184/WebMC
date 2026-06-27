package java.nio.channels;

public abstract class FileLock implements java.io.Closeable {
    protected FileLock(FileChannel channel, long position, long size, boolean shared) {}
    public final FileChannel channel() { return null; }
    public final long position() { return 0; }
    public final long size() { return 0; }
    public final boolean isShared() { return false; }
    public final boolean overlaps(long position, long size) { return false; }
    public abstract boolean isValid();
    public abstract void release();
    @Override public void close() { release(); }
}
