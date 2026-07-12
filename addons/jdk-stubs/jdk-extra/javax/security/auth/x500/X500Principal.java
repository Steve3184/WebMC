package javax.security.auth.x500;

public class X500Principal {
    private final String name;

    public X500Principal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getName(String format) {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof X500Principal)) return false;
        return name.equals(((X500Principal) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
