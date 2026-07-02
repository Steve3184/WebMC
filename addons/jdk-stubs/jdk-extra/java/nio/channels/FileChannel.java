package java.nio.channels;

public abstract class FileChannel implements java.io.Closeable {
    protected FileChannel() {}
    public abstract int read(java.nio.ByteBuffer dst);
    public abstract int write(java.nio.ByteBuffer src);
    public abstract long size();
    public abstract long position();
    public abstract FileChannel position(long newPosition);
    public abstract FileChannel truncate(long size);
    public abstract void force(boolean metaData);
    public abstract long transferTo(long position, long count, java.nio.channels.WritableByteChannel target);
    public abstract long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count);
    public abstract int read(java.nio.ByteBuffer dst, long position);
    public abstract int write(java.nio.ByteBuffer src, long position);
    public abstract java.nio.MappedByteBuffer map(MapMode mode, long position, long size);
    public abstract FileLock lock(long position, long size, boolean shared);
    public final FileLock lock() { return lock(0L, Long.MAX_VALUE, false); }
    public abstract FileLock tryLock(long position, long size, boolean shared);
    public final FileLock tryLock() { return tryLock(0L, Long.MAX_VALUE, false); }
    @Override public abstract void close();
    public boolean isOpen() { return true; }

    public static class MapMode {
        public static final MapMode READ_ONLY = new MapMode("READ_ONLY");
        public static final MapMode READ_WRITE = new MapMode("READ_WRITE");
        public static final MapMode PRIVATE = new MapMode("PRIVATE");
        private final String name;
        private MapMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
}
