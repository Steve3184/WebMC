package java.nio.file.attribute;

import java.util.concurrent.TimeUnit;

public final class FileTime implements Comparable<FileTime> {
    private final long value;
    private final TimeUnit unit;
    private FileTime(long value, TimeUnit unit) { this.value = value; this.unit = unit; }
    public static FileTime from(long value, TimeUnit unit) { return new FileTime(value, unit); }
    public static FileTime fromMillis(long value) { return new FileTime(value, TimeUnit.MILLISECONDS); }
    public static FileTime from(java.time.Instant instant) { return new FileTime(instant.toEpochMilli(), TimeUnit.MILLISECONDS); }
    public long to(TimeUnit unit) { return unit.convert(value, this.unit); }
    public long toMillis() { return TimeUnit.MILLISECONDS.convert(value, unit); }
    public java.time.Instant toInstant() { return java.time.Instant.ofEpochMilli(toMillis()); }
    @Override public int compareTo(FileTime other) { return Long.compare(toMillis(), other.toMillis()); }
    @Override public boolean equals(Object obj) { return obj instanceof FileTime && ((FileTime) obj).toMillis() == toMillis(); }
    @Override public int hashCode() { return Long.hashCode(toMillis()); }
    @Override public String toString() { return java.time.Instant.ofEpochMilli(toMillis()).toString(); }
}
