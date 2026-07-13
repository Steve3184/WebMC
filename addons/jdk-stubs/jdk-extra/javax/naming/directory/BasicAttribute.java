package javax.naming.directory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BasicAttribute implements Attribute {
    private final String attrID;
    private final List<Object> values = new ArrayList<>();
    private boolean ordered = false;

    public BasicAttribute(String attrID) {
        this.attrID = attrID;
    }

    public BasicAttribute(String attrID, Object value) {
        this.attrID = attrID;
        if (value != null) {
            this.values.add(value);
        }
    }

    @Override
    public Object get() {
        return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Enumeration<Object> getAll() {
        return new java.util.Enumeration<Object>() {
            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                return index < values.size();
            }

            @Override
            public Object nextElement() {
                return values.get(index++);
            }
        };
    }

    @Override
    public String getID() {
        return attrID != null ? attrID : "";
    }

    @Override
    public boolean contains(Object attrVal) {
        return values.contains(attrVal);
    }

    @Override
    public boolean add(Object attrVal) {
        return values.add(attrVal);
    }

    @Override
    public boolean remove(Object attrval) {
        return values.remove(attrval);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Object get(int index) {
        return values.get(index);
    }

    @Override
    public Object remove(int index) {
        return values.remove(index);
    }

    @Override
    public java.util.List<Object> toList() {
        return new ArrayList<>(values);
    }

    @Override
    public Object clone() {
        return this;
    }
}
