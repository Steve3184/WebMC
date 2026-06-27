package com.mojang.logging;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Browser-side replacement for com.mojang.logging.LogUtils.
 *
 * Critical: must NOT touch org.slf4j.MarkerFactory or LoggerFactory at
 * static init time. Both walk SLF4J's ServiceLoader provider chain, which
 * TeaVM's getResources() emulation returns null for, causing
 * "Cannot read properties of null (reading '$hasMoreElements')". Instead
 * we hand-roll a Marker and a Logger that go straight to System.err.
 */
public class LogUtils {
    public static final String FATAL_MARKER_ID = "FATAL";
    public static final Marker FATAL_MARKER = new BasicMarker(FATAL_MARKER_ID);

    public static boolean isLoggerActive() {
        return true;
    }

    public static void configureRootLoggingLevel(final org.slf4j.event.Level level) {
        // no-op
    }

    public static Object defer(final Supplier<Object> result) {
        class ToString {
            @Override public String toString() { return result.get().toString(); }
        }
        return new ToString();
    }

    public static Logger getLogger() {
        top.steve3184.webmc.web.WebMcShadowMarker.touch();
        return new ConsoleLogger("mc");
    }
}
