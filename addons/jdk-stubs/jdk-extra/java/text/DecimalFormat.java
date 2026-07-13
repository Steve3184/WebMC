package java.text;

public class DecimalFormat extends NumberFormat {
    private String pattern;
    private java.text.DecimalFormatSymbols symbols;
    private int multiplier = 1;
    private boolean groupingUsed = true;
    private int groupingSize = 3;
    private int minIntegerDigits = 1;
    private int maxIntegerDigits = 40;
    private int minFractionDigits = 0;
    private int maxFractionDigits = 3;
    private boolean parseIntegerOnly = false;

    public DecimalFormat() {
        this.symbols = new DecimalFormatSymbols();
        this.pattern = "#,##0.###";
    }

    public DecimalFormat(String pattern) {
        this.pattern = pattern;
        this.symbols = new DecimalFormatSymbols();
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this.pattern = pattern;
        this.symbols = symbols;
    }

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition pos) {
        String numStr = String.valueOf(number);
        int dotIndex = numStr.indexOf('.');
        if (dotIndex < 0) dotIndex = numStr.length();
        String intPart = numStr.substring(0, dotIndex);
        String fracPart = dotIndex < numStr.length() ? numStr.substring(dotIndex + 1) : "";
        if (groupingUsed && intPart.length() > groupingSize) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < intPart.length(); i++) {
                if (i > 0 && (intPart.length() - i) % groupingSize == 0) {
                    sb.append(symbols.getGroupingSeparator());
                }
                sb.append(intPart.charAt(i));
            }
            intPart = sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (fracPart.length() < minFractionDigits) {
            while (sb.length() < minFractionDigits - fracPart.length()) sb.append('0');
        }
        sb.append(fracPart.substring(0, Math.min(fracPart.length(), maxFractionDigits)));
        return result.append(intPart).append(symbols.getDecimalSeparator()).append(sb);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition pos) {
        return format((double) number, result, pos);
    }

    @Override
    public Number parse(String source, ParsePosition pos) {
        int start = pos.getIndex();
        int index = start;
        boolean negative = false;
        if (index < source.length() && source.charAt(index) == '-') {
            negative = true;
            index++;
        }
        long intValue = 0;
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
            intValue = intValue * 10 + (source.charAt(index) - '0');
            index++;
        }
        if (index < source.length() && source.charAt(index) == symbols.getDecimalSeparator()) {
            index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }
        pos.setIndex(index);
        long value = negative ? -intValue : intValue;
        return Long.valueOf(value * multiplier);
    }

    @Override
    public Number parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    public String getPattern() {
        return pattern;
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return symbols;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols symbols) {
        this.symbols = symbols;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    @Override
    public void setGroupingUsed(boolean groupingUsed) {
        this.groupingUsed = groupingUsed;
    }

    @Override
    public int getGroupingSize() {
        return groupingSize;
    }

    @Override
    public void setGroupingSize(int groupingSize) {
        this.groupingSize = groupingSize;
    }

    @Override
    public int getMaximumIntegerDigits() {
        return maxIntegerDigits;
    }

    @Override
    public void setMaximumIntegerDigits(int newValue) {
        maxIntegerDigits = Math.max(0, newValue);
    }

    @Override
    public int getMinimumIntegerDigits() {
        return minIntegerDigits;
    }

    @Override
    public void setMinimumIntegerDigits(int newValue) {
        minIntegerDigits = Math.max(0, newValue);
    }

    @Override
    public int getMaximumFractionDigits() {
        return maxFractionDigits;
    }

    @Override
    public void setMaximumFractionDigits(int newValue) {
        maxFractionDigits = Math.max(0, newValue);
    }

    @Override
    public int getMinimumFractionDigits() {
        return minFractionDigits;
    }

    @Override
    public void setMinimumFractionDigits(int newValue) {
        minFractionDigits = Math.max(0, newValue);
    }

    @Override
    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    @Override
    public Object clone() {
        DecimalFormat other = (DecimalFormat) super.clone();
        other.symbols = (DecimalFormatSymbols) symbols.clone();
        return other;
    }

    @Override
    public int hashCode() {
        return pattern.hashCode() ^ symbols.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DecimalFormat)) return false;
        DecimalFormat other = (DecimalFormat) obj;
        return pattern.equals(other.pattern) && symbols.equals(other.symbols);
    }
}
