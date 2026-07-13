package java.security.cert;

public abstract class X509Certificate extends Certificate {
    protected X509Certificate() { super("X.509"); }

    public static X509Certificate getInstance(java.io.InputStream inStream) throws CertificateException { return null; }
    public static X509Certificate getInstance(byte[] certData) throws CertificateException { return null; }

    public abstract void checkValidity();
    public abstract void checkValidity(java.util.Date date);
    public abstract int getVersion();
    public abstract java.math.BigInteger getSerialNumber();
    public abstract java.security.Principal getIssuerDN();
    public abstract java.security.Principal getSubjectDN();
    public abstract java.util.Date getNotBefore();
    public abstract java.util.Date getNotAfter();
    public abstract byte[] getTBSCertificate();
    public abstract byte[] getSignature();
    public abstract String getSigAlgName();
    public abstract String getSigAlgOID();
    public abstract byte[] getSigAlgParams();
    public abstract boolean[] getIssuerUniqueID();
    public abstract boolean[] getSubjectUniqueID();
    public abstract boolean[] getKeyUsage();
    public abstract int getBasicConstraints();

    public java.security.PublicKey getPublicKey() { return null; }
    public byte[] getEncoded() { return new byte[0]; }
    public void verify(java.security.PublicKey key) {}
    public void verify(java.security.PublicKey key, String sigProvider) {}
    public String toString() { return "X509Certificate"; }

    public java.security.Principal getSubjectX500Principal() { return null; }
    public java.security.Principal getIssuerX500Principal() { return null; }
    public java.util.List<?> getSubjectAlternativeNames() { return null; }
    public java.util.List<?> getIssuerAlternativeNames() { return null; }
}
