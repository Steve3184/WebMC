package org.apache.logging.log4j;

public class MarkerManager {
    private MarkerManager() {}

    public static Marker getMarker(String name) {
        return new Marker(name);
    }

    public static class Marker {
        private final String name;
        private Marker parent;
        private java.util.Set<Marker> references;

        public Marker(String name) {
            this.name = name;
            this.references = new java.util.HashSet<>();
        }

        public String getName() {
            return name;
        }

        public void addParent(Marker parent) {
            this.parent = parent;
        }

        public Marker getParent() {
            return parent;
        }

        public java.util.List<Marker> getParents() {
            java.util.List<Marker> parents = new java.util.ArrayList<>();
            if (parent != null) {
                parents.add(parent);
            }
            return parents;
        }

        public boolean hasParents() {
            return parent != null;
        }

        public boolean remove(Marker marker) {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Marker)) return false;
            return name.equals(((Marker) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
