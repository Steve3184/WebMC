package java.text;

public class Format {
    public static class Field {
        private final String name;

        public Field(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Field)) return false;
            return name.equals(((Field) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    public String format(Object obj) {
        return String.valueOf(obj);
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
        toAppendTo.append(format(obj));
        return toAppendTo;
    }

    public Object parseObject(String source) {
        return null;
    }

    public Object parseObject(String source, java.text.ParsePosition pos) {
        return null;
    }
}
