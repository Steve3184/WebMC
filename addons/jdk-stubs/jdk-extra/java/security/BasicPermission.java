package java.security;

public abstract class BasicPermission extends Permission {
    private final String actions;
    public BasicPermission(String name) { super(name); actions = ""; }
    public BasicPermission(String name, String actions) { super(name); this.actions = actions == null ? "" : actions; }
    @Override public boolean implies(Permission p) { return false; }
    @Override public boolean equals(Object o) { return o instanceof BasicPermission && ((BasicPermission) o).getName().equals(getName()); }
    @Override public int hashCode() { return getName().hashCode(); }
    @Override public String getActions() { return actions; }
}
