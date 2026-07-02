package javax.security.cert;

/** Legacy javax.security.cert.X509Certificate (deprecated, replaced by java.security.cert).
 *  Stub for completeness — referenced from old SSL code paths but not used in browser. */
public abstract class X509Certificate extends Certificate {
    protected X509Certificate() {}
    public abstract byte[] getEncoded();
    public abstract void verify(java.security.PublicKey key);
    public abstract void verify(java.security.PublicKey key, String sigProvider);
    public abstract String toString();
    public abstract java.security.PublicKey getPublicKey();
    public abstract void checkValidity();
    public abstract void checkValidity(java.util.Date date);
    public abstract int getVersion();
    public abstract java.math.BigInteger getSerialNumber();
    public abstract java.security.Principal getIssuerDN();
    public abstract java.security.Principal getSubjectDN();
    public abstract java.util.Date getNotBefore();
    public abstract java.util.Date getNotAfter();
    public abstract String getSigAlgName();
    public abstract String getSigAlgOID();
    public abstract byte[] getSigAlgParams();
}
