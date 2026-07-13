package java.text;

import java.util.Locale;

public class DateFormatSymbols {
    private String[] eras;
    private String[] months;
    private String[] shortMonths;
    private String[] weekdays;
    private String[] shortWeekdays;
    private String[] ampms;
    private String[][] zoneStrings;
    private String localPatternChars;

    public DateFormatSymbols() {
        initialize();
    }

    public DateFormatSymbols(Locale locale) {
        initialize();
    }

    private void initialize() {
        eras = new String[] {"BC", "AD"};
        months = new String[] {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        shortMonths = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        weekdays = new String[] {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        shortWeekdays = new String[] {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        ampms = new String[] {"AM", "PM"};
        zoneStrings = new String[0][];
        localPatternChars = "GyMdkHmsSEDFwWahKz";
    }

    public String[] getEras() {
        return eras.clone();
    }

    public void setEras(String[] eras) {
        this.eras = eras.clone();
    }

    public String[] getMonths() {
        return months.clone();
    }

    public void setMonths(String[] months) {
        this.months = months.clone();
    }

    public String[] getShortMonths() {
        return shortMonths.clone();
    }

    public void setShortMonths(String[] shortMonths) {
        this.shortMonths = shortMonths.clone();
    }

    public String[] getWeekdays() {
        return weekdays.clone();
    }

    public void setWeekdays(String[] weekdays) {
        this.weekdays = weekdays.clone();
    }

    public String[] getShortWeekdays() {
        return shortWeekdays.clone();
    }

    public void setShortWeekdays(String[] shortWeekdays) {
        this.shortWeekdays = shortWeekdays.clone();
    }

    public String[] getAmPmStrings() {
        return ampms.clone();
    }

    public void setAmPmStrings(String[] ampms) {
        this.ampms = ampms.clone();
    }

    public String[][] getZoneStrings() {
        return zoneStrings.clone();
    }

    public void setZoneStrings(String[][] zoneStrings) {
        this.zoneStrings = zoneStrings.clone();
    }

    public String getLocalPatternChars() {
        return localPatternChars;
    }

    public void setLocalPatternChars(String localPatternChars) {
        this.localPatternChars = localPatternChars;
    }

    public static Locale[] getAvailableLocales() {
        return new Locale[] { Locale.getDefault() };
    }

    public static DateFormatSymbols getInstance() {
        return new DateFormatSymbols();
    }

    public static DateFormatSymbols getInstance(Locale locale) {
        return new DateFormatSymbols(locale);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DateFormatSymbols)) return false;
        DateFormatSymbols other = (DateFormatSymbols) obj;
        return java.util.Arrays.equals(eras, other.eras) &&
               java.util.Arrays.equals(months, other.months);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
