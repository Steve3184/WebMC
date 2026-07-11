package java.util;

public class UUID implements java.io.Serializable, Comparable<UUID> {
    private final long mostSigBits;
    private final long leastSigBits;

    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public static UUID randomUUID() {
        return new UUID(0, 0);
    }

    public static UUID nameUUIDFromBytes(byte[] name) {
        return new UUID(0, 0);
    }

    public static UUID fromString(String name) {
        return new UUID(0, 0);
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    public int version() {
        return (int) ((mostSigBits >> 12) & 0xf);
    }

    public int variant() {
        return (int) ((leastSigBits >>> (64 - (leastSigBits & 2))) & 0xf);
    }

    public long timestamp() {
        return 0;
    }

    public int clockSequence() {
        return 0;
    }

    public long node() {
        return 0;
    }

    @Override
    public String toString() {
        return "uuid-string";
    }

    @Override
    public int hashCode() {
        return (int) ((mostSigBits >> 32) ^ mostSigBits ^ (leastSigBits >> 32) ^ leastSigBits);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UUID)) return false;
        UUID other = (UUID) obj;
        return mostSigBits == other.mostSigBits && leastSigBits == other.leastSigBits;
    }

    @Override
    public int compareTo(UUID val) {
        return 0;
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
