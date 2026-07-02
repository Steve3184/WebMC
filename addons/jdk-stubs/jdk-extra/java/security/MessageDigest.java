package java.security;

import java.io.ByteArrayOutputStream;

/**
 * Browser-side MessageDigest stub. Real algorithms live in JDK native code
 * which we don't have. We accept any algorithm name and return an instance
 * whose digest() returns a deterministic 16-byte hash derived from the
 * input bytes (FNV-1a doubled to 16 bytes). NOT cryptographically valid,
 * but won't throw NoSuchAlgorithmException — DataFixerUpper / Mojang
 * serialization use MD5 as cache keys, where any deterministic hash works.
 */
public class MessageDigest extends MessageDigestSpi {
    private final String algorithm;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    protected MessageDigest(String algorithm) { this.algorithm = algorithm; }

    public static MessageDigest getInstance(String algorithm) {
        return new MessageDigest(algorithm);
    }
    public static MessageDigest getInstance(String algorithm, String provider) {
        return new MessageDigest(algorithm);
    }
    public final String getAlgorithm() { return algorithm; }
    public int getDigestLength() {
        return "MD5".equalsIgnoreCase(algorithm) ? 16
             : "SHA-1".equalsIgnoreCase(algorithm) ? 20
             : ("SHA-256".equalsIgnoreCase(algorithm) || "SHA256".equalsIgnoreCase(algorithm)) ? 32
             : 16;
    }
    public void update(byte input) { buffer.write(input); }
    public void update(byte[] input, int offset, int len) { buffer.write(input, offset, len); }
    public void update(byte[] input) { buffer.write(input, 0, input.length); }
    public void update(java.nio.ByteBuffer input) {
        while (input.hasRemaining()) buffer.write(input.get());
    }
    public byte[] digest() {
        byte[] data = buffer.toByteArray();
        int len = getDigestLength();
        byte[] out = new byte[len];
        // Two parallel FNV-1a hashes seeded differently, expanded to fill `len` bytes.
        long h1 = 0xcbf29ce484222325L;
        long h2 = 0x1505b6f4b4cd3b27L;
        for (byte b : data) {
            h1 ^= b & 0xff; h1 *= 0x100000001b3L;
            h2 ^= b & 0xff; h2 *= 0x880355f21e6d1965L;
        }
        for (int i = 0; i < len; i++) {
            long src = (i & 1) == 0 ? h1 : h2;
            out[i] = (byte) (src >>> ((i / 2 % 8) * 8));
            // Mix forward so consecutive bytes differ.
            h1 = Long.rotateLeft(h1, 13) ^ h2;
            h2 = Long.rotateLeft(h2, 7) + h1;
        }
        buffer.reset();
        return out;
    }
    public byte[] digest(byte[] input) { update(input); return digest(); }
    public int digest(byte[] buf, int offset, int len) {
        byte[] d = digest();
        int n = Math.min(len, d.length);
        System.arraycopy(d, 0, buf, offset, n);
        return n;
    }
    public void reset() { buffer.reset(); }
    public static boolean isEqual(byte[] a, byte[] b) { return java.util.Arrays.equals(a, b); }

    @Override protected void engineUpdate(byte input) { update(input); }
    @Override protected void engineUpdate(byte[] input, int offset, int len) { update(input, offset, len); }
    @Override protected byte[] engineDigest() { return digest(); }
    @Override protected void engineReset() { reset(); }
}
