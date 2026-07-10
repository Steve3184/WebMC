package top.steve3184.webmc.teavm.glfw;

public final class WindowBackendHolder {
    private static WindowBackend backend;

    private WindowBackendHolder() {
    }

    public static void install(WindowBackend backend) {
        WindowBackendHolder.backend = backend;
    }

    public static WindowBackend get() {
        return backend;
    }

    public static WindowBackend current() {
        if (backend == null) {
            throw new IllegalStateException("WindowBackend not installed");
        }
        return backend;
    }
}
