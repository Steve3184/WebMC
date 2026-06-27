package java.security;

public class CodeSource implements java.io.Serializable {
    private final java.net.URL location;
    public CodeSource(java.net.URL url, java.security.cert.Certificate[] certs) { this.location = url; }
    public CodeSource(java.net.URL url, java.security.cert.CodeSigner[] signers) { this.location = url; }
    public final java.net.URL getLocation() { return location; }
    public final java.security.cert.Certificate[] getCertificates() { return null; }
    public final java.security.cert.CodeSigner[] getCodeSigners() { return null; }
    public boolean implies(CodeSource codesource) { return false; }
    @Override public boolean equals(Object obj) { return obj == this; }
    @Override public int hashCode() { return location == null ? 0 : location.hashCode(); }
}
