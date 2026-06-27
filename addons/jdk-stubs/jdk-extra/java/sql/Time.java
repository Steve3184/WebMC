package java.sql;

public class Time extends java.util.Date {
    public Time(long time) { super(time); }
    public Time(int hour, int minute, int second) { super(70, 0, 1, hour, minute, second); }
    public static Time valueOf(String s) { return new Time(0L); }
    public java.time.LocalTime toLocalTime() { return java.time.LocalTime.now(); }
    public static Time valueOf(java.time.LocalTime time) { return new Time(0L); }
    @Override public String toString() { return "00:00:00"; }
}
