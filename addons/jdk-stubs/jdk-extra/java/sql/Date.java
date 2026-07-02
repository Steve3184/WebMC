package java.sql;

public class Date extends java.util.Date {
    public Date(long date) { super(date); }
    public Date(int year, int month, int day) { super(year, month, day); }
    public static Date valueOf(String s) { return new Date(0L); }
    @Override public String toString() { return "1970-01-01"; }
}
