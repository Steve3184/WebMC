package top.steve3184.webmc.teavm;

import org.teavm.jso.JSBody;

/**
 * Unified logging utility for WebMC TeaVM build.
 * Provides consistent logging across all modules.
 */
public final class WebLog {

    private WebLog() {}

    /**
     * Log info message.
     */
    public static void info(String msg) {
        log("[WebMC] " + msg);
    }

    /**
     * Log warning message.
     */
    public static void warn(String msg) {
        log("[WebMC WARN] " + msg);
    }

    /**
     * Log error message.
     */
    public static void error(String msg) {
        log("[WebMC ERROR] " + msg);
    }

    /**
     * Log debug message.
     */
    public static void debug(String msg) {
        log("[WebMC DEBUG] " + msg);
    }

    @JSBody(params = {"msg"}, script =
        "if (typeof console !== 'undefined' && console !== null) { " +
        "  console.log(msg);" +
        "}"
    )
    private static native void log(String msg);
}
