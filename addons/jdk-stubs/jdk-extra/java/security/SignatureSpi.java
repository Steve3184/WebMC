package java.security;

public abstract class SignatureSpi {
    protected abstract void engineInitVerify(PublicKey publicKey);
    protected abstract void engineInitSign(PrivateKey privateKey);
    protected abstract void engineUpdate(byte b);
    protected abstract void engineUpdate(byte[] b, int off, int len);
    protected abstract byte[] engineSign();
    protected abstract boolean engineVerify(byte[] sigBytes);
    protected void engineSetParameter(String param, Object value) {}
    protected Object engineGetParameter(String param) { return null; }
}
