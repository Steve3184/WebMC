package java.nio.channels;

public abstract class SeekableByteChannel implements ByteChannel {
    protected SeekableByteChannel() {}
    public abstract long position();
    public abstract SeekableByteChannel position(long newPosition);
    public abstract long size();
    public abstract SeekableByteChannel truncate(long size);
}
