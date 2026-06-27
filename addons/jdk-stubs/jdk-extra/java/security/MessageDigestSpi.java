package java.security;

public abstract class MessageDigestSpi {
    protected abstract void engineUpdate(byte input);
    protected abstract void engineUpdate(byte[] input, int offset, int len);
    protected void engineUpdate(java.nio.ByteBuffer input) {}
    protected abstract byte[] engineDigest();
    protected int engineDigest(byte[] buf, int offset, int len) { return 0; }
    protected abstract void engineReset();
    protected int engineGetDigestLength() { return 0; }
}
