package java.security;

public class KeyStore {
    private KeyStore() {}
    public static KeyStore getInstance(String type) throws KeyStoreException {
        throw new KeyStoreException("KeyStore not available in browser");
    }
    public static String getDefaultType() { return "PKCS12"; }
    public final void load(java.io.InputStream stream, char[] password) {}
    public final void load(LoadStoreParameter param) {}
    public final void store(java.io.OutputStream stream, char[] password) {}
    public final void store(LoadStoreParameter param) {}
    public final java.util.Enumeration<String> aliases() { return java.util.Collections.emptyEnumeration(); }
    public final boolean containsAlias(String alias) { return false; }
    public final int size() { return 0; }
    public interface LoadStoreParameter {}
    public interface ProtectionParameter {}
    public interface Entry {}
}
