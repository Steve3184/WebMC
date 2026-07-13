package java.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ObjectStreamField implements Comparable<Object> {
    private final String name;
    private final Class<?> type;
    private final Field field;
    private final int offset;
    
    public ObjectStreamField(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.field = null;
        this.offset = 0;
    }
    
    public ObjectStreamField(Field field) {
        this.field = field;
        this.name = field.getName();
        this.type = field.getType();
        this.offset = 0;
    }
    
    public String getName() { return name; }
    public Class<?> getType() { return type; }
    public int getOffset() { return offset; }
    public Field getField() { return field; }
    public boolean isPrimitive() { return type.isPrimitive(); }
    public int compareTo(Object obj) { return 0; }
}
