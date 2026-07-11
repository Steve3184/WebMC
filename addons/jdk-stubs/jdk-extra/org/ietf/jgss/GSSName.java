package org.ietf.jgss;

public class GSSName {
    public static final Oid NT_HOSTBASED_SERVICE;
    static {
        Oid _nt = null;
        try { _nt = new Oid("1.3.6.1.5.6.2.9"); } catch (GSSException e) {}
        NT_HOSTBASED_SERVICE = _nt;
    }

    public String toString() {
        return "GSSName";
    }

    public GSSName canonicalize(Oid oid) throws GSSException {
        return this;
    }
}
