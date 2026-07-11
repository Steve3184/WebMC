package java.text;

import java.util.Currency;
import java.util.Locale;

public class DecimalFormatSymbols {
    private char decimalSeparator = '.';
    private char groupingSeparator = ',';
    private String infinity = "∞";
    private String NaN = "NaN";
    private char minusSign = '-';
    private char monetaryDecimalSeparator;
    private char zeroDigit = '0';
    private String patternSeparator = ";";
    private char percent = '%';
    private char perMill = '‰';
    private char digit = '#';
    private String exponentSeparator = "E";
    private String localPatternSeparator = ";";
    private Currency currency;
    private String currencySymbol = "$";
    private String internationalCurrencySymbol = "USD";
    private char padEscape = '*';
    private char minusSignChar = '-';

    public DecimalFormatSymbols() {
        this(Locale.getDefault());
    }

    public DecimalFormatSymbols(Locale locale) {
        currency = Currency.getInstance(locale);
        currencySymbol = currency.getSymbol(locale);
        internationalCurrencySymbol = currency.getCurrencyCode();
    }

    public static DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    public static DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    public static Locale[] getAvailableLocales() {
        return new Locale[] { Locale.getDefault() };
    }

    public char getDecimalSeparator() { return decimalSeparator; }
    public void setDecimalSeparator(char s) { this.decimalSeparator = s; }
    public char getGroupingSeparator() { return groupingSeparator; }
    public void setGroupingSeparator(char s) { this.groupingSeparator = s; }
    public String getInfinity() { return infinity; }
    public void setInfinity(String s) { this.infinity = s; }
    public String getNaN() { return NaN; }
    public void setNaN(String s) { this.NaN = s; }
    public char getMinusSign() { return minusSign; }
    public void setMinusSign(char s) { this.minusSign = s; }
    public char getMonetaryDecimalSeparator() { return monetaryDecimalSeparator != 0 ? monetaryDecimalSeparator : decimalSeparator; }
    public void setMonetaryDecimalSeparator(char s) { this.monetaryDecimalSeparator = s; }
    public char getZeroDigit() { return zeroDigit; }
    public void setZeroDigit(char d) { this.zeroDigit = d; }
    public String getPatternSeparator() { return patternSeparator; }
    public void setPatternSeparator(String s) { this.patternSeparator = s; }
    public char getPercent() { return percent; }
    public void setPercent(char c) { this.percent = c; }
    public char getPerMill() { return perMill; }
    public void setPerMill(char c) { this.perMill = c; }
    public char getDigit() { return digit; }
    public void setDigit(char d) { this.digit = d; }
    public String getExponentSeparator() { return exponentSeparator; }
    public void setExponentSeparator(String s) { this.exponentSeparator = s; }
    public String getLocalPatternSeparator() { return localPatternSeparator; }
    public void setLocalPatternSeparator(String s) { this.localPatternSeparator = s; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency c) { this.currency = c; }
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String s) { this.currencySymbol = s; }
    public String getInternationalCurrencySymbol() { return internationalCurrencySymbol; }
    public void setInternationalCurrencySymbol(String s) { this.internationalCurrencySymbol = s; }
    public char getPadEscape() { return padEscape; }
    public void setPadEscape(char c) { this.padEscape = c; }

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
        if (!(obj instanceof DecimalFormatSymbols)) return false;
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        return decimalSeparator == other.decimalSeparator && groupingSeparator == other.groupingSeparator;
    }

    @Override
    public int hashCode() {
        return decimalSeparator ^ groupingSeparator;
    }
}
