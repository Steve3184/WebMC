package java.io;

import java.nio.channels.FileChannel;

public class FileInputStream extends InputStream {
    private final String path;
    private FileDescriptor fd;
    private FileChannel channel;
    private boolean closed = false;

    public FileInputStream(String name) throws FileNotFoundException {
        this.path = name;
        this.fd = new FileDescriptor();
    }

    public FileInputStream(FileDescriptor fdObj) {
        this.path = null;
        this.fd = fdObj;
    }

    public FileInputStream(File file) throws FileNotFoundException {
        this.path = file.getPath();
        this.fd = new FileDescriptor();
    }

    @Override
    public int read() throws IOException {
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return -1;
    }

    @Override
    public long skip(long n) throws IOException {
        return 0;
    }

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    public final FileDescriptor getFD() throws IOException {
        return fd;
    }

    public FileChannel getChannel() {
        return null;
    }

    protected void finalize() throws IOException {
        if (!closed) {
            close();
        }
    }
}
