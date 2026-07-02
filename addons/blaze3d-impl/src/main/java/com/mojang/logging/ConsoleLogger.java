package com.mojang.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import top.steve3184.webmc.web.BuildFlags;
import top.steve3184.webmc.web.WebDiagnostics;

/**
 * Bare-bones SLF4J Logger that emits to System.out — TeaVM maps that to
 * console.info in the browser. Slf4j's NOPLogger silences everything,
 * which made it impossible to see what MC was doing during boot.
 *
 * Implements only the format method overloads MC actually calls. Marker
 * variants reuse the unmarked path.
 */
public class ConsoleLogger implements Logger {
    private final String name;

    // mc-web: yield to the browser every N log lines so the tab stays
    // responsive during boot (which emits thousands of log lines before
    // entering the render loop, and has no other natural yield points in
    // the synchronous TeaVM coroutine).
    private static int yieldCounter = 0;
    private static final int YIELD_EVERY = 32;

    public ConsoleLogger(String name) { this.name = name; }
    public ConsoleLogger() { this("slf4j"); }

    @Override public String getName() { return name; }

    private void emit(String level, String msg) {
        if (BuildFlags.WEB_RUNTIME && !WebDiagnostics.enabled() && isWebDebugMessage(msg)) {
            return;
        }
        System.out.println("[" + level + "] " + name + ": " + msg);
        if (++yieldCounter >= YIELD_EVERY) {
            yieldCounter = 0;
            try { Thread.sleep(0); } catch (InterruptedException ignored) {}
        }
    }

    private static boolean isWebDebugMessage(String msg) {
        return msg != null && (msg.startsWith("[mc-web/") || msg.startsWith("[mc-probe]") || msg.startsWith("[mc-main-stage]"));
    }

    private void emit(String level, String fmt, Object... args) {
        org.slf4j.helpers.FormattingTuple tuple = MessageFormatter.arrayFormat(fmt, args);
        emit(level, tuple.getMessage());
        Throwable t = tuple.getThrowable();
        if (t != null) t.printStackTrace(System.out);
    }

    private void emit(String level, String msg, Throwable t) {
        emit(level, msg);
        if (t != null) t.printStackTrace(System.out);
    }

    // ── isXEnabled ────────────────────────────────────────────────────
    @Override public boolean isTraceEnabled() { return false; }
    @Override public boolean isTraceEnabled(Marker marker) { return false; }
    @Override public boolean isDebugEnabled() { return false; }
    @Override public boolean isDebugEnabled(Marker marker) { return false; }
    @Override public boolean isInfoEnabled() { return true; }
    @Override public boolean isInfoEnabled(Marker marker) { return true; }
    @Override public boolean isWarnEnabled() { return true; }
    @Override public boolean isWarnEnabled(Marker marker) { return true; }
    @Override public boolean isErrorEnabled() { return true; }
    @Override public boolean isErrorEnabled(Marker marker) { return true; }

    // ── trace ─────────────────────────────────────────────────────────
    @Override public void trace(String msg) {}
    @Override public void trace(String fmt, Object arg) {}
    @Override public void trace(String fmt, Object a1, Object a2) {}
    @Override public void trace(String fmt, Object... args) {}
    @Override public void trace(String msg, Throwable t) {}
    @Override public void trace(Marker m, String msg) {}
    @Override public void trace(Marker m, String fmt, Object arg) {}
    @Override public void trace(Marker m, String fmt, Object a1, Object a2) {}
    @Override public void trace(Marker m, String fmt, Object... args) {}
    @Override public void trace(Marker m, String msg, Throwable t) {}

    // ── debug ─────────────────────────────────────────────────────────
    @Override public void debug(String msg) {}
    @Override public void debug(String fmt, Object arg) {}
    @Override public void debug(String fmt, Object a1, Object a2) {}
    @Override public void debug(String fmt, Object... args) {}
    @Override public void debug(String msg, Throwable t) {}
    @Override public void debug(Marker m, String msg) {}
    @Override public void debug(Marker m, String fmt, Object arg) {}
    @Override public void debug(Marker m, String fmt, Object a1, Object a2) {}
    @Override public void debug(Marker m, String fmt, Object... args) {}
    @Override public void debug(Marker m, String msg, Throwable t) {}

    // ── info ──────────────────────────────────────────────────────────
    @Override public void info(String msg) { emit("INFO", msg); }
    @Override public void info(String fmt, Object arg) { emit("INFO", fmt, arg); }
    @Override public void info(String fmt, Object a1, Object a2) { emit("INFO", fmt, a1, a2); }
    @Override public void info(String fmt, Object... args) { emit("INFO", fmt, args); }
    @Override public void info(String msg, Throwable t) { emit("INFO", msg, t); }
    @Override public void info(Marker m, String msg) { info(msg); }
    @Override public void info(Marker m, String fmt, Object arg) { info(fmt, arg); }
    @Override public void info(Marker m, String fmt, Object a1, Object a2) { info(fmt, a1, a2); }
    @Override public void info(Marker m, String fmt, Object... args) { info(fmt, args); }
    @Override public void info(Marker m, String msg, Throwable t) { info(msg, t); }

    // ── warn ──────────────────────────────────────────────────────────
    @Override public void warn(String msg) { emit("WARN", msg); }
    @Override public void warn(String fmt, Object arg) { emit("WARN", fmt, arg); }
    @Override public void warn(String fmt, Object... args) { emit("WARN", fmt, args); }
    @Override public void warn(String fmt, Object a1, Object a2) { emit("WARN", fmt, a1, a2); }
    @Override public void warn(String msg, Throwable t) { emit("WARN", msg, t); }
    @Override public void warn(Marker m, String msg) { warn(msg); }
    @Override public void warn(Marker m, String fmt, Object arg) { warn(fmt, arg); }
    @Override public void warn(Marker m, String fmt, Object a1, Object a2) { warn(fmt, a1, a2); }
    @Override public void warn(Marker m, String fmt, Object... args) { warn(fmt, args); }
    @Override public void warn(Marker m, String msg, Throwable t) { warn(msg, t); }

    // ── error ─────────────────────────────────────────────────────────
    @Override public void error(String msg) { emit("ERROR", msg); }
    @Override public void error(String fmt, Object arg) { emit("ERROR", fmt, arg); }
    @Override public void error(String fmt, Object a1, Object a2) { emit("ERROR", fmt, a1, a2); }
    @Override public void error(String fmt, Object... args) { emit("ERROR", fmt, args); }
    @Override public void error(String msg, Throwable t) { emit("ERROR", msg, t); }
    @Override public void error(Marker m, String msg) { error(msg); }
    @Override public void error(Marker m, String fmt, Object arg) { error(fmt, arg); }
    @Override public void error(Marker m, String fmt, Object a1, Object a2) { error(fmt, a1, a2); }
    @Override public void error(Marker m, String fmt, Object... args) { error(fmt, args); }
    @Override public void error(Marker m, String msg, Throwable t) { error(msg, t); }
}
