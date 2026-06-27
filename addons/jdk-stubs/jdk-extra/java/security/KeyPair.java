package java.security;

public final class KeyPair implements java.io.Serializable {
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    public KeyPair(PublicKey publicKey, PrivateKey privateKey) { this.publicKey = publicKey; this.privateKey = privateKey; }
    public PublicKey getPublic() { return publicKey; }
    public PrivateKey getPrivate() { return privateKey; }
}
