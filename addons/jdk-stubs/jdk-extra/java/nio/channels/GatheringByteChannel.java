package java.nio.channels;

public interface GatheringByteChannel extends WritableByteChannel {
    long write(java.nio.ByteBuffer[] srcs, int offset, int length);
    long write(java.nio.ByteBuffer[] srcs);
}
