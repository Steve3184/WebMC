package org.fusesource.jansi;

public class AnsiRenderer {
    public static String render(String input) { return input; }
    public static String renderCodes(String codes) { return ""; }
    public static String renderCodes(String... codes) { return ""; }
    public enum Code {
        BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE, DEFAULT,
        FG_BLACK, FG_RED, FG_GREEN, FG_YELLOW, FG_BLUE, FG_MAGENTA, FG_CYAN, FG_WHITE,
        BG_BLACK, BG_RED, BG_GREEN, BG_YELLOW, BG_BLUE, BG_MAGENTA, BG_CYAN, BG_WHITE,
        BOLD, FAINT, ITALIC, UNDERLINE, BLINK_SLOW, BLINK_FAST, NEGATIVE_ON, CONCEAL_ON, STRIKETHROUGH_ON,
        RESET;
        public boolean isColor() { return false; }
        public boolean isBackground() { return false; }
        public boolean isAttribute() { return false; }
        public Object getValue() { return null; }
    }
}
