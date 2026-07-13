package javax.naming.ldap;

import java.util.List;
import java.util.ArrayList;

public class Rdn implements java.io.Serializable, java.lang.Comparable<Rdn> {
    private final String type;
    private final Object value;

    public Rdn(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Rdn() {
        this.type = "";
        this.value = "";
    }

    public Rdn(String name) {
        this.type = "";
        this.value = name;
    }

    public String getType() {
        return type != null ? type : "";
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type + "=" + value;
    }

    public List<Rdn> getRdns() {
        return new ArrayList<>();
    }

    @Override
    public int compareTo(Rdn another) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rdn)) return false;
        Rdn other = (Rdn) o;
        return java.util.Objects.equals(type, other.type) &&
               java.util.Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value);
    }
}
