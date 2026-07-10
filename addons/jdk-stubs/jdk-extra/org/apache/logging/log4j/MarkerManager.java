package org.apache.logging.log4j;
public class MarkerManager {
    private MarkerManager() {}
    public static Marker getMarker(String name) { return new MarkerImpl(name); }

    public interface Marker {
        String getName();
        boolean add(Marker marker);
        boolean remove(Marker marker);
        boolean hasReferences();
        java.util.Iterator<Marker> iterator();
        boolean has(Marker marker);
    }

    private static class MarkerImpl implements Marker {
        private final String name;
        private final java.util.List<Marker> references = new java.util.ArrayList<>();
        MarkerImpl(String name) { this.name = name; }
        public String getName() { return name; }
        public boolean add(Marker marker) { return references.add(marker); }
        public boolean remove(Marker marker) { return references.remove(marker); }
        public boolean hasReferences() { return !references.isEmpty(); }
        public java.util.Iterator<Marker> iterator() { return references.iterator(); }
        public boolean has(Marker marker) { return references.contains(marker); }
    }
}
