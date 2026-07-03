package top.steve3184.webmc.web;

/**
 * Web diagnostics utilities for TeaVM web runtime.
 */
public final class WebDiagnostics {
    private static final boolean ENABLED = false;

    /** Returns true if web diagnostics should emit verbose debug output */
    public static boolean enabled() {
        return ENABLED;
    }

    private WebDiagnostics() {} // no instance
}
