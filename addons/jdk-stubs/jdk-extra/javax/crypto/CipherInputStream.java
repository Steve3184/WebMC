package javax.crypto;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CipherInputStream extends InputStream {
    private InputStream in;
    private javax.crypto.Cipher cipher;

    public CipherInputStream(InputStream in, javax.crypto.Cipher cipher) {
        this.in = in;
        this.cipher = cipher;
    }

    @Override
    public int read() { return -1; }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
