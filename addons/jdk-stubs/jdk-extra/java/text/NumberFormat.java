package java.text;

import java.util.Locale;

public abstract class NumberFormat extends Format {
    public static final int INTEGER_FIELD = 0;
    public static final int FRACTION_FIELD = 1;

    protected NumberFormat() {}

    public static NumberFormat getInstance() {
        return getNumberInstance();
    }

    public static NumberFormat getInstance(Locale locale) {
        return getNumberInstance(locale);
    }

    public static NumberFormat getNumberInstance() {
        return new DecimalFormat();
    }

    public static NumberFormat getNumberInstance(Locale locale) {
        return new DecimalFormat();
    }

    public static NumberFormat getIntegerInstance() {
        return new DecimalFormat();
    }

    public static NumberFormat getIntegerInstance(Locale locale) {
        return new DecimalFormat();
    }

    public static NumberFormat getCurrencyInstance() {
        return new DecimalFormat();
    }

    public static NumberFormat getCurrencyInstance(Locale locale) {
        return new DecimalFormat();
    }

    public static NumberFormat getPercentInstance() {
        return new DecimalFormat();
    }

    public static NumberFormat getPercentInstance(Locale locale) {
        return new DecimalFormat();
    }

    public static Locale[] getAvailableLocales() {
        return new Locale[] { Locale.getDefault() };
    }

    public String format(long number) {
        return format((double) number);
    }

    public String format(double number) {
        StringBuffer sb = new StringBuffer();
        format(number, sb, new FieldPosition(0));
        return sb.toString();
    }

    public abstract StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos);
    public abstract StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos);
    public abstract Number parse(String source, ParsePosition parsePosition);
    public abstract Object parseObject(String source, ParsePosition pos);

    public int hashCode() { return 0; }
    public boolean equals(Object obj) { return this == obj; }
    public Object clone() { return this; }

    public boolean isGroupingUsed() { return true; }
    public void setGroupingUsed(boolean newValue) {}

    public int getGroupingSize() { return 3; }
    public void setGroupingSize(int size) {}

    public int getMaximumIntegerDigits() { return 40; }
    public void setMaximumIntegerDigits(int newValue) {}
    public int getMinimumIntegerDigits() { return 1; }
    public void setMinimumIntegerDigits(int newValue) {}
    public int getMaximumFractionDigits() { return 3; }
    public void setMaximumFractionDigits(int newValue) {}
    public int getMinimumFractionDigits() { return 0; }
    public void setMinimumFractionDigits(int newValue) {}

    public boolean isParseIntegerOnly() { return false; }
    public void setParseIntegerOnly(boolean value) {}

    public java.util.Currency getCurrency() { return null; }
    public void setCurrency(java.util.Currency currency) {}

    public RoundingMode getRoundingMode() { return RoundingMode.HALF_UP; }
    public void setRoundingMode(RoundingMode roundingMode) {}

    public static class Field extends Format.Field {
        public static final Field INTEGER = new Field("integer");
        public static final Field FRACTION = new Field("fraction");
        public static final Field EXPONENT = new Field("exponent");
        public static final Field DECIMAL_SEPARATOR = new Field("decimal separator");
        public static final Field EXPONENT_SIGN = new Field("exponent sign");
        public static final Field EXPONENT_SYMBOL = new Field("exponent symbol");
        public static final Field GROUPING_SEPARATOR = new Field("grouping separator");

        protected Field(String name) { super(name); }
    }

    public enum RoundingMode {
        UP, DOWN, CEILING, FLOOR, HALF_UP, HALF_DOWN, HALF_EVEN, UNNECESSARY
    }
}
