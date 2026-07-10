package top.steve3184.webmc.web;

import org.teavm.jso.JSBody;

/**
 * Web diagnostics utilities for TeaVM web runtime.
 */
public final class WebDiagnostics {
    private static final boolean ENABLED = false;

    /** Returns true if web diagnostics should emit verbose debug output */
    public static boolean enabled() {
        return ENABLED;
    }

    /**
     * Emit a Performance.measure() entry for timeline diagnostics.
     * No-op when diagnostics are disabled.
     */
    public static void timelineEvent(String category, String phase, String detail, int durationMs, long startMs) {
        if (!ENABLED) return;
        emitTimelineEvent(category, phase, detail, durationMs, startMs);
    }

    @JSBody(params = {"category", "phase", "detail", "durationMs", "startMs"}, script =
        "try {" +
        "  if (typeof performance !== 'undefined' && performance.mark && performance.measure) {" +
        "    const label = category + ':' + phase + ':' + detail;" +
        "    performance.mark(label + '-start');" +
        "    performance.mark(label + '-end');" +
        "    performance.measure(label, label + '-start', label + '-end');" +
        "  }" +
        "} catch(e) { console.warn('[WebDiagnostics]', e); }")
    private static native void emitTimelineEvent(String category, String phase, String detail, int durationMs, long startMs);

    private WebDiagnostics() {} // no instance
}
