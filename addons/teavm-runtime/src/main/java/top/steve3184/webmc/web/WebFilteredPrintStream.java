package top.steve3184.webmc.web;

import java.io.PrintStream;

public final class WebFilteredPrintStream extends PrintStream {
    private final PrintStream delegate;

    private WebFilteredPrintStream(PrintStream delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public static void install() {
        if (!BuildFlags.WEB_RUNTIME) {
            return;
        }
        if (!(System.out instanceof WebFilteredPrintStream)) {
            System.setOut(new WebFilteredPrintStream(System.out));
        }
        if (!(System.err instanceof WebFilteredPrintStream)) {
            System.setErr(new WebFilteredPrintStream(System.err));
        }
    }

    @Override
    public void println(String value) {
        if (shouldDrop(value)) {
            return;
        }
        this.delegate.println(value);
    }

    @Override
    public void println(Object value) {
        String text = String.valueOf(value);
        if (shouldDrop(text)) {
            return;
        }
        this.delegate.println(value);
    }

    @Override
    public void print(String value) {
        if (shouldDrop(value)) {
            return;
        }
        this.delegate.print(value);
    }

    @Override
    public void print(Object value) {
        String text = String.valueOf(value);
        if (shouldDrop(text)) {
            return;
        }
        this.delegate.print(value);
    }

    private static boolean shouldDrop(String value) {
        if (WebDiagnostics.enabled()) {
            return false;
        }
        if (value == null) {
            return false;
        }
        return value.startsWith("[mc-web/")
            || value.startsWith("[mc-probe]")
            || value.startsWith("[mc-main-stage]")
            || value.startsWith("[stdout-test]")
            || value.startsWith("[stderr-test]")
            || value.startsWith("[INFO] mc: [mc-web/")
            || value.startsWith("[WARN] mc: [mc-web/")
            || value.startsWith("[ERROR] mc: [mc-web/");
    }
}
