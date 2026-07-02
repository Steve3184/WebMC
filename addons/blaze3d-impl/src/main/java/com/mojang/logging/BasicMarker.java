package com.mojang.logging;

import java.util.Iterator;
import org.slf4j.Marker;

/** Minimal Marker — avoids org.slf4j.MarkerFactory which would pull in
 *  SLF4J's ServiceLoader provider discovery (broken on TeaVM). */
class BasicMarker implements Marker {
    private final String name;
    BasicMarker(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public void add(Marker reference) {}
    @Override public boolean remove(Marker reference) { return false; }
    @Override public boolean hasChildren() { return false; }
    @Override public boolean hasReferences() { return false; }
    @Override public Iterator<Marker> iterator() { return java.util.Collections.<Marker>emptyList().iterator(); }
    @Override public boolean contains(Marker other) { return name.equals(other.getName()); }
    @Override public boolean contains(String name) { return this.name.equals(name); }
    @Override public boolean equals(Object obj) { return obj instanceof Marker && name.equals(((Marker) obj).getName()); }
    @Override public int hashCode() { return name.hashCode(); }
    @Override public String toString() { return name; }
}
