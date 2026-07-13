package java.text;

import java.util.Date;

public abstract class DateFormat {
    public abstract StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition);
    public abstract Date parse(String source, ParsePosition pos);
    public final String format(Date date) { return format(date, new StringBuffer(), new FieldPosition(0)).toString(); }
    public Date parse(String source) throws java.text.ParseException { return parse(source, new ParsePosition(0)); }

    public static final int ERA_FIELD = 0;
    public static final int YEAR_FIELD = 1;
    public static final int MONTH_FIELD = 2;
    public static final int DATE_FIELD = 3;
    public static final int HOUR_OF_DAY1_FIELD = 4;
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int MINUTE_FIELD = 6;
    public static final int SECOND_FIELD = 7;
    public static final int MILLISECOND_FIELD = 8;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int AM_PM_FIELD = 14;
    public static final int HOUR1_FIELD = 15;
    public static final int HOUR0_FIELD = 16;
    public static final int TIMEZONE_FIELD = 17;

    public static final int FULL = 0;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    public static final int DEFAULT = MEDIUM;

    public static DateFormat getDateInstance() { return new SimpleDateFormat(); }
    public static DateFormat getDateInstance(int style) { return new SimpleDateFormat(); }
    public static DateFormat getDateInstance(int style, java.util.Locale locale) { return new SimpleDateFormat(); }
    public static DateFormat getTimeInstance() { return new SimpleDateFormat(); }
    public static DateFormat getTimeInstance(int style) { return new SimpleDateFormat(); }
    public static DateFormat getTimeInstance(int style, java.util.Locale locale) { return new SimpleDateFormat(); }
    public static DateFormat getDateTimeInstance() { return new SimpleDateFormat(); }
    public static DateFormat getDateTimeInstance(int dateStyle, int timeStyle) { return new SimpleDateFormat(); }
    public static DateFormat getDateTimeInstance(int dateStyle, int timeStyle, java.util.Locale locale) { return new SimpleDateFormat(); }
    public static DateFormat getInstance() { return getDateTimeInstance(SHORT, SHORT); }

    public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }

    public abstract java.util.Calendar getCalendar();
    public abstract void setCalendar(java.util.Calendar newCalendar);
    public abstract java.util.TimeZone getTimeZone();
    public abstract void setTimeZone(java.util.TimeZone zone);
    public abstract boolean isLenient();
    public abstract void setLenient(boolean lenient);

    @Override
    public Object clone() { return this; }

    public static class Field extends java.text.Format.Field {
        public static final Field ERA = new Field("era");
        public static final Field YEAR = new Field("year");
        public static final Field MONTH = new Field("month");
        public static final Field DAY_OF_MONTH = new Field("day of month");
        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1");
        public static final Field HOUR_OF_DAY0 = new Field("hour of day 0");
        public static final Field MINUTE = new Field("minute");
        public static final Field SECOND = new Field("second");
        public static final Field MILLISECOND = new Field("millisecond");
        public static final Field DAY_OF_WEEK = new Field("day of week");
        public static final Field DAY_OF_YEAR = new Field("day of year");
        public static final Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month");
        public static final Field WEEK_OF_YEAR = new Field("week of year");
        public static final Field WEEK_OF_MONTH = new Field("week of month");
        public static final Field AM_PM = new Field("am pm");
        public static final Field HOUR1 = new Field("hour 1");
        public static final Field HOUR0 = new Field("hour 0");
        public static final Field TIME_ZONE = new Field("time zone");

        protected Field(String name) { super(name); }
    }
}
