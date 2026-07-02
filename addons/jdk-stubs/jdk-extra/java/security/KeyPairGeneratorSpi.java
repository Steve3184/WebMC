package java.security;

public abstract class KeyPairGeneratorSpi {
    public abstract void initialize(int keysize);
    public abstract KeyPair generateKeyPair();
}
