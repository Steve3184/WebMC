package java.io;

public class ObjectStreamClass implements java.io.Serializable {
    private final Class<?> cl;
    private ObjectStreamClass(Class<?> cl) { this.cl = cl; }
    public static ObjectStreamClass lookup(Class<?> cl) { return new ObjectStreamClass(cl); }
    public static ObjectStreamClass lookupAny(Class<?> cl) { return lookup(cl); }
    public String getName() { return cl == null ? "" : cl.getName(); }
    public long getSerialVersionUID() { return 0L; }
    public Class<?> forClass() { return cl; }
}
