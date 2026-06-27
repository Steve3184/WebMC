package org.apache.log4j;

public class Priority {
    private final String name;
    private final int value;
    protected Priority(String name, int value) { this.name = name; this.value = value; }
    public final String toString() { return name; }
    public final int toInt() { return value; }
    public boolean isGreaterOrEqual(Priority p) { return p == null || value >= p.value; }
    public boolean equals(Object o) { return o instanceof Priority && ((Priority) o).value == value; }
    public int hashCode() { return value; }
}
