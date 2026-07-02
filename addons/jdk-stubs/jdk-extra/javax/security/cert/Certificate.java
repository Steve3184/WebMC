package javax.security.cert;

public abstract class Certificate {
    public abstract byte[] getEncoded();
    public abstract void verify(java.security.PublicKey key);
    public abstract void verify(java.security.PublicKey key, String sigProvider);
    public abstract String toString();
    public abstract java.security.PublicKey getPublicKey();
}
