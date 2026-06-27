package java.security;

public abstract class KeyFactorySpi {
    public abstract PublicKey engineGeneratePublic(java.security.spec.KeySpec keySpec);
    public abstract PrivateKey engineGeneratePrivate(java.security.spec.KeySpec keySpec);
    public abstract <T extends java.security.spec.KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec);
    public abstract Key engineTranslateKey(Key key);
}
