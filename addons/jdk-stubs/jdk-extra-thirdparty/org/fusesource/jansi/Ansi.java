package org.fusesource.jansi;

public class Ansi {
    public static Ansi ansi() { return new Ansi(); }
    public Ansi a(Object o) { return this; }
    public Ansi reset() { return this; }
    public Ansi fg(Color c) { return this; }
    public Ansi bg(Color c) { return this; }
    public Ansi fgBright(Color c) { return this; }
    public Ansi bgBright(Color c) { return this; }
    public Ansi bold() { return this; }
    public Ansi boldOff() { return this; }
    public Ansi newline() { return this; }
    @Override public String toString() { return ""; }
    public enum Color { BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE, DEFAULT }
    public enum Attribute { RESET, INTENSITY_BOLD, INTENSITY_FAINT, ITALIC, UNDERLINE, BLINK_SLOW, BLINK_FAST, NEGATIVE_ON, CONCEAL_ON, STRIKETHROUGH_ON, UNDERLINE_DOUBLE }
    public enum Erase { FORWARD, BACKWARD, ALL }
}
