package java.sql;

public class Timestamp extends java.util.Date {
    private int nanos;
    public Timestamp(long time) { super(time); this.nanos = (int) ((time % 1000L) * 1_000_000L); }
    public Timestamp(int year, int month, int date, int hour, int minute, int second, int nano) {
        super(year, month, date, hour, minute, second);
        this.nanos = nano;
    }
    public static Timestamp valueOf(String s) { return new Timestamp(0L); }
    public static Timestamp from(java.time.Instant instant) { return new Timestamp(instant.toEpochMilli()); }
    public java.time.Instant toInstant() { return java.time.Instant.ofEpochMilli(getTime()); }
    public java.time.LocalDateTime toLocalDateTime() { return java.time.LocalDateTime.now(); }
    public int getNanos() { return nanos; }
    public void setNanos(int n) { this.nanos = n; }
    public boolean before(Timestamp ts) { return getTime() < ts.getTime(); }
    public boolean after(Timestamp ts) { return getTime() > ts.getTime(); }
    public int compareTo(Timestamp ts) { return Long.compare(getTime(), ts.getTime()); }
    @Override public String toString() { return "1970-01-01 00:00:00"; }
}
