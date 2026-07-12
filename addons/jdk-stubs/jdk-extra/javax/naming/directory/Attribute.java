package javax.naming.directory;

import java.util.Enumeration;

public interface Attribute {
    Object get();
    int size();
    Enumeration<Object> getAll();
    String getID();
    boolean contains(Object attrVal);
    boolean add(Object attrVal);
    boolean remove(Object attrval);
    void clear();
    Object get(int index);
    Object remove(int index);
    java.util.List<Object> toList();
    Object clone();
}
