package java.io;

import java.nio.channels.FileChannel;

public class FileOutputStream extends OutputStream {
    private final String path;
    private FileDescriptor fd;
    private FileChannel channel;
    private boolean closed = false;

    public FileOutputStream(String name) throws FileNotFoundException {
        this.path = name;
        this.fd = new FileDescriptor();
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this.path = name;
        this.fd = new FileDescriptor();
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this.path = file.getPath();
        this.fd = new FileDescriptor();
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        this.path = file.getPath();
        this.fd = new FileDescriptor();
    }

    public FileOutputStream(FileDescriptor fdObj) {
        this.path = null;
        this.fd = fdObj;
    }

    @Override
    public void write(int b) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
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

    @SuppressWarnings("removal")
    protected void finalize() throws IOException {
        if (!closed) {
            try {
                close();
            } catch (IOException ignored) {}
        }
    }
}
