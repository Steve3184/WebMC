package java.util;

public final class StringJoiner {
    private final CharSequence prefix;
    private final CharSequence suffix;
    private final CharSequence delimiter;
    private final StringBuilder body = new StringBuilder();
    private boolean empty = true;
    private CharSequence emptyValue;

    public StringJoiner(CharSequence delimiter) { this(delimiter, "", ""); }
    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        this.delimiter = delimiter; this.prefix = prefix; this.suffix = suffix;
        this.emptyValue = "" + prefix + suffix;
    }
    public StringJoiner setEmptyValue(CharSequence emptyValue) { this.emptyValue = emptyValue.toString(); return this; }
    public StringJoiner add(CharSequence newElement) {
        if (empty) { body.append(prefix); empty = false; } else body.append(delimiter);
        body.append(newElement == null ? "null" : newElement);
        return this;
    }
    public StringJoiner merge(StringJoiner other) {
        if (other != null && other.body.length() > 0) {
            String s = other.body.toString() + other.suffix;
            add(s.substring(other.prefix.length()));
        }
        return this;
    }
    public int length() { return empty ? emptyValue.length() : body.length() + suffix.length(); }
    @Override public String toString() {
        if (empty) return emptyValue.toString();
        return body.toString() + suffix;
    }
}
