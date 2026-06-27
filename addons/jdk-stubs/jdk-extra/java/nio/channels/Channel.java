package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;

public interface Channel extends Closeable {
    boolean isOpen();
    @Override
    void close() throws IOException;
}
