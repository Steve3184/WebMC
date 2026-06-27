package java.util;

public final class HexFormat {
    private static final HexFormat OF = new HexFormat();
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private HexFormat() {}
    public static HexFormat of() { return OF; }
    public static HexFormat ofDelimiter(String delimiter) { return OF; }
    public HexFormat withDelimiter(String delimiter) { return this; }
    public HexFormat withPrefix(String prefix) { return this; }
    public HexFormat withSuffix(String suffix) { return this; }
    public HexFormat withUpperCase() { return this; }
    public HexFormat withLowerCase() { return this; }
    public String formatHex(byte[] bytes) { return formatHex(bytes, 0, bytes.length); }
    public String formatHex(byte[] bytes, int fromIndex, int toIndex) {
        StringBuilder sb = new StringBuilder((toIndex - fromIndex) * 2);
        for (int i = fromIndex; i < toIndex; i++) {
            int v = bytes[i] & 0xff;
            sb.append(HEX[v >>> 4]).append(HEX[v & 0xf]);
        }
        return sb.toString();
    }
    public byte[] parseHex(CharSequence string) {
        int len = string.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("odd length");
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((digit(string.charAt(i)) << 4) | digit(string.charAt(i + 1)));
        }
        return out;
    }
    public static int fromHexDigit(int ch) { return digit((char) ch); }
    public static int fromHexDigits(CharSequence s) {
        int v = 0;
        for (int i = 0; i < s.length(); i++) v = (v << 4) | digit(s.charAt(i));
        return v;
    }
    public static int fromHexDigits(CharSequence s, int fromIndex, int toIndex) {
        int v = 0;
        for (int i = fromIndex; i < toIndex; i++) v = (v << 4) | digit(s.charAt(i));
        return v;
    }
    public static long fromHexDigitsToLong(CharSequence s) {
        long v = 0L;
        for (int i = 0; i < s.length(); i++) v = (v << 4) | digit(s.charAt(i));
        return v;
    }
    public static long fromHexDigitsToLong(CharSequence s, int fromIndex, int toIndex) {
        long v = 0L;
        for (int i = fromIndex; i < toIndex; i++) v = (v << 4) | digit(s.charAt(i));
        return v;
    }
    public static boolean isHexDigit(int ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }
    // Returns String — the JDK declares toHexDigits(byte) -> String. Earlier
    // version returned char[] which was wrong (different bytecode descriptor).
    public String toHexDigits(byte b) {
        return new String(new char[] { HEX[(b >>> 4) & 0xf], HEX[b & 0xf] });
    }
    public String toHexDigits(char c) {
        return new String(new char[] {
            HEX[(c >>> 12) & 0xf], HEX[(c >>> 8) & 0xf],
            HEX[(c >>> 4) & 0xf], HEX[c & 0xf]
        });
    }
    public String toHexDigits(short s) {
        return new String(new char[] {
            HEX[(s >>> 12) & 0xf], HEX[(s >>> 8) & 0xf],
            HEX[(s >>> 4) & 0xf], HEX[s & 0xf]
        });
    }
    public String toHexDigits(int v) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 28; i >= 0; i -= 4) sb.append(HEX[(v >>> i) & 0xf]);
        return sb.toString();
    }
    public String toHexDigits(long v) {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 60; i >= 0; i -= 4) sb.append(HEX[(int) (v >>> i) & 0xf]);
        return sb.toString();
    }
    public String toHexDigits(byte... bytes) { return formatHex(bytes); }
    private static int digit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        throw new IllegalArgumentException("not hex: " + c);
    }
    public String delimiter() { return ""; }
    public String prefix() { return ""; }
    public String suffix() { return ""; }
    public boolean isUpperCase() { return false; }
}
