package javax.naming.ldap;

import java.util.List;
import java.util.ArrayList;

public class LdapName implements java.lang.Comparable<LdapName> {
    private final String parsedName;

    public LdapName(String name) {
        this.parsedName = name;
    }

    public List<String> getComponents() {
        return new ArrayList<>();
    }

    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return parsedName != null ? parsedName : "";
    }

    public List<Rdn> getRdns() {
        return new ArrayList<>();
    }

    @Override
    public int compareTo(LdapName another) {
        return 0;
    }

    public String getParserClassName() {
        return "";
    }
}
