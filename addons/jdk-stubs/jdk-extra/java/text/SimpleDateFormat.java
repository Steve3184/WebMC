package java.text;

import java.util.Date;
import java.util.Locale;

public class SimpleDateFormat extends DateFormat {
    private String pattern;
    private DateFormatSymbols formatData;
    private java.util.Calendar calendar;

    public SimpleDateFormat() {
        this("MM/dd/yy", Locale.getDefault());
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        this.pattern = pattern;
        this.formatData = new DateFormatSymbols(locale);
        this.calendar = java.util.Calendar.getInstance(locale);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatData) {
        this.pattern = pattern;
        this.formatData = formatData;
        this.calendar = java.util.Calendar.getInstance();
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        calendar.setTime(date);
        StringBuffer sb = toAppendTo;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case 'y': sb.append(calendar.get(java.util.Calendar.YEAR)); break;
                case 'M': sb.append(calendar.get(java.util.Calendar.MONTH) + 1); break;
                case 'd': sb.append(calendar.get(java.util.Calendar.DAY_OF_MONTH)); break;
                case 'H': sb.append(calendar.get(java.util.Calendar.HOUR_OF_DAY)); break;
                case 'h': sb.append(calendar.get(java.util.Calendar.HOUR)); break;
                case 'm': sb.append(calendar.get(java.util.Calendar.MINUTE)); break;
                case 's': sb.append(calendar.get(java.util.Calendar.SECOND)); break;
                case 'S': sb.append(calendar.get(java.util.Calendar.MILLISECOND)); break;
                case 'E': sb.append(formatData.getWeekdays()[calendar.get(java.util.Calendar.DAY_OF_WEEK)]); break;
                case 'a': sb.append(formatData.getAmPmStrings()[calendar.get(java.util.Calendar.AM_PM)]); break;
                default: sb.append(c); break;
            }
        }
        return sb;
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        try {
            int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;
            int index = pos.getIndex();
            for (int i = 0; i < pattern.length() && index < source.length(); i++) {
                char pc = pattern.charAt(i);
                if (Character.isLetter(pc)) {
                    int start = index;
                    while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
                    String num = source.substring(start, index);
                    int value = num.isEmpty() ? 0 : Integer.parseInt(num);
                    switch (pc) {
                        case 'y': year = value; break;
                        case 'M': month = value - 1; break;
                        case 'd': day = value; break;
                        case 'H': case 'h': hour = value; break;
                        case 'm': minute = value; break;
                        case 's': second = value; break;
                    }
                } else {
                    if (source.charAt(index) == pc) index++;
                }
            }
            pos.setIndex(index);
            calendar.set(year, month, day, hour, minute, second);
            return calendar.getTime();
        } catch (Exception e) {
            pos.setErrorIndex(pos.getIndex());
            return null;
        }
    }

    @Override
    public java.util.Calendar getCalendar() {
        return calendar;
    }

    @Override
    public void setCalendar(java.util.Calendar newCalendar) {
        this.calendar = newCalendar;
    }

    @Override
    public java.util.TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    @Override
    public void setTimeZone(java.util.TimeZone zone) {
        calendar.setTimeZone(zone);
    }

    @Override
    public boolean isLenient() {
        return calendar.isLenient();
    }

    @Override
    public void setLenient(boolean lenient) {
        calendar.setLenient(lenient);
    }

    @Override
    public Object clone() {
        return new SimpleDateFormat(pattern, (DateFormatSymbols) formatData.clone());
    }

    public String getPattern() {
        return pattern;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) formatData.clone();
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
    }

    public void applyLocalizedPattern(String pattern) {
        applyPattern(pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleDateFormat)) return false;
        SimpleDateFormat other = (SimpleDateFormat) obj;
        return pattern.equals(other.pattern) && formatData.equals(other.formatData);
    }
}
