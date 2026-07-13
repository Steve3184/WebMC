package javax.naming.directory;

import java.util.*;

public interface Attributes {
    Attribute get(String attrID);
    Attribute get(javax.naming.BufferID attrID);
    javax.naming.NamingEnumeration<? extends Attribute> getAll();
    javax.naming.NamingEnumeration<String> getIDs();
    boolean isCaseIgnored();
    Attribute put(String attrID, Object val);
    Attribute put(Attribute attr);
    Attribute remove(String attrID);
    int size();
    Object clone();
}
