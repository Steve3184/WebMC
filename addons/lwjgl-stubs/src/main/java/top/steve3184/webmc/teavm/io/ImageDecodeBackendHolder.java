package top.steve3184.webmc.teavm.io;

public final class ImageDecodeBackendHolder {
    private static ImageDecodeBackend backend;

    private ImageDecodeBackendHolder() {}

    public static void install(ImageDecodeBackend backend) {
        ImageDecodeBackendHolder.backend = backend;
    }

    public static ImageDecodeBackend current() {
        if (backend == null) {
            throw new IllegalStateException("ImageDecodeBackend not installed");
        }
        return backend;
    }
}
