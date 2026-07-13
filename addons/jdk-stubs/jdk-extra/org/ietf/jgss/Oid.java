package org.ietf.jgss;
import java.io.InputStream;
public class Oid {
    private final String value;
    public Oid(String value) throws GSSException { this.value = value; }
    public Oid(InputStream der) throws GSSException { this.value = "1.2.3"; }
    public String toString() { return value; }
    public boolean equals(Object obj) { return obj instanceof Oid && ((Oid) obj).value.equals(value); }
    public int hashCode() { return value.hashCode(); }
}
