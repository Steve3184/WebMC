package java.nio.channels;

public interface ScatteringByteChannel extends ReadableByteChannel {
    long read(java.nio.ByteBuffer[] dsts, int offset, int length);
    long read(java.nio.ByteBuffer[] dsts);
}
