package javax.crypto;

public class Cipher {
    public static final int ENCRYPT_MODE = 1;
    public static final int DECRYPT_MODE = 2;
    public static final int WRAP_MODE = 3;
    public static final int UNWRAP_MODE = 4;
    private Cipher() {}
    public static Cipher getInstance(String transformation) throws java.security.NoSuchAlgorithmException {
        throw new java.security.NoSuchAlgorithmException("Cipher not available in browser: " + transformation);
    }
    public final String getAlgorithm() { return ""; }
    public final void init(int opmode, java.security.Key key) {}
    public final void init(int opmode, java.security.Key key, java.security.SecureRandom random) {}
    public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params) {}
    public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params, java.security.SecureRandom random) {}
    public final byte[] update(byte[] input) { return new byte[0]; }
    public final byte[] update(byte[] input, int inputOffset, int inputLen) { return new byte[0]; }
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output) { return 0; }
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) { return 0; }
    public final byte[] doFinal() { return new byte[0]; }
    public final byte[] doFinal(byte[] input) { return new byte[0]; }
    public final byte[] doFinal(byte[] input, int inputOffset, int inputLen) { return new byte[0]; }
    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) { return 0; }
}
