package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;

public final class Channels {
    private Channels() {}

    public static ReadableByteChannel newChannel(InputStream in) {
        if (in == null) throw new NullPointerException("in");
        return new InputStreamChannel(in);
    }

    public static WritableByteChannel newChannel(OutputStream out) {
        if (out == null) throw new NullPointerException("out");
        return new OutputStreamChannel(out);
    }

    public static InputStream newInputStream(ReadableByteChannel ch) { throw new UnsupportedOperationException(); }
    public static OutputStream newOutputStream(WritableByteChannel ch) { throw new UnsupportedOperationException(); }
    public static Reader newReader(ReadableByteChannel ch, java.nio.charset.CharsetDecoder dec, int minBufferCap) { throw new UnsupportedOperationException(); }
    public static Reader newReader(ReadableByteChannel ch, String csName) { throw new UnsupportedOperationException(); }
    public static Writer newWriter(WritableByteChannel ch, java.nio.charset.CharsetEncoder enc, int minBufferCap) { throw new UnsupportedOperationException(); }
    public static Writer newWriter(WritableByteChannel ch, String csName) { throw new UnsupportedOperationException(); }

    private static final class InputStreamChannel implements ReadableByteChannel {
        private final InputStream in;
        private boolean open = true;

        InputStreamChannel(InputStream in) { this.in = in; }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if (!open) throw new ClosedChannelException();
            int remaining = dst.remaining();
            if (remaining == 0) return 0;
            if (dst.hasArray()) {
                int n = in.read(dst.array(), dst.arrayOffset() + dst.position(), remaining);
                if (n > 0) dst.position(dst.position() + n);
                return n;
            }
            // Direct buffer path: read into a scratch array then copy.
            byte[] scratch = new byte[Math.min(remaining, 8192)];
            int n = in.read(scratch, 0, scratch.length);
            if (n > 0) dst.put(scratch, 0, n);
            return n;
        }

        @Override public boolean isOpen() { return open; }
        @Override public void close() throws IOException { open = false; in.close(); }
    }

    private static final class OutputStreamChannel implements WritableByteChannel {
        private final OutputStream out;
        private boolean open = true;

        OutputStreamChannel(OutputStream out) { this.out = out; }

        @Override
        public int write(ByteBuffer src) throws IOException {
            if (!open) throw new ClosedChannelException();
            int remaining = src.remaining();
            if (remaining == 0) return 0;
            if (src.hasArray()) {
                out.write(src.array(), src.arrayOffset() + src.position(), remaining);
                src.position(src.position() + remaining);
                return remaining;
            }
            byte[] scratch = new byte[remaining];
            src.get(scratch);
            out.write(scratch);
            return remaining;
        }

        @Override public boolean isOpen() { return open; }
        @Override public void close() throws IOException { open = false; out.close(); }
    }
}
