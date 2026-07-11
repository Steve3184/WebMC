package org.ietf.jgss;

public abstract class GSSManager {
    private static GSSManager me = new DummyGSSManager();

    public static GSSManager getInstance() {
        return me;
    }

    public Oid[] getMechs() {
        return new Oid[0];
    }

    public Oid[] getNamesForMech(Oid mech) {
        return new Oid[0];
    }

    public GSSName createName(String nameStr, Oid nameType) throws GSSException {
        return null;
    }

    public GSSCredential createCredential(GSSName name, int lifetime, Oid mech, int usage) throws GSSException {
        return null;
    }

    public GSSContext createContext(GSSName peer, Oid mech, GSSCredential myCred, int lifetime) throws GSSException {
        return null;
    }

    private static class DummyGSSManager extends GSSManager {
    }
}
