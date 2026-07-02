package java.util;

/**
 * Minimal Scanner — Mojang code uses Scanner for parsing simple inputs.
 * This stub covers basic int/string reads from a String input source.
 */
public final class Scanner implements java.util.Iterator<String>, java.io.Closeable {
    private final String src;
    private int pos = 0;
    public Scanner(java.io.InputStream src) { this.src = ""; }
    public Scanner(java.io.InputStream src, String charsetName) { this.src = ""; }
    public Scanner(java.io.Reader src) { this.src = ""; }
    public Scanner(String src) { this.src = src == null ? "" : src; }
    public Scanner(java.io.File src) { this.src = ""; }
    public Scanner(java.nio.file.Path src) { this.src = ""; }
    public Scanner(Readable src) { this.src = ""; }

    @Override public boolean hasNext() { skipWs(); return pos < src.length(); }
    @Override public String next() {
        skipWs();
        int start = pos;
        while (pos < src.length() && !Character.isWhitespace(src.charAt(pos))) pos++;
        return src.substring(start, pos);
    }
    public boolean hasNextInt() { skipWs(); int p = pos; if (p>=src.length()) return false; char c = src.charAt(p); return c == '-' || Character.isDigit(c); }
    public int nextInt() { return Integer.parseInt(next()); }
    public boolean hasNextLong() { return hasNextInt(); }
    public long nextLong() { return Long.parseLong(next()); }
    public boolean hasNextDouble() { skipWs(); int p = pos; return p < src.length(); }
    public double nextDouble() { return Double.parseDouble(next()); }
    public boolean hasNextLine() { skipWs(); return pos < src.length(); }
    public String nextLine() {
        int start = pos;
        while (pos < src.length() && src.charAt(pos) != '\n') pos++;
        String s = src.substring(start, pos);
        if (pos < src.length()) pos++;
        return s;
    }
    public Scanner useDelimiter(String pattern) { return this; }
    public Scanner useDelimiter(java.util.regex.Pattern pattern) { return this; }
    @Override public void close() {}
    private void skipWs() { while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }
}
