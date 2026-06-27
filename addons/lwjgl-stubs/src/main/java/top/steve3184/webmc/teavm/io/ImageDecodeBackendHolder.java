package top.steve3184.webmc.teavm.io;

public final class ImageDecodeBackendHolder {
    private static ImageDecodeBackend INSTANCE;
    private ImageDecodeBackendHolder() {}
    public static void install(ImageDecodeBackend b) {
        if (INSTANCE != null) throw new IllegalStateException("ImageDecodeBackend already installed");
        INSTANCE = b;
    }
    public static ImageDecodeBackend current() {
        if (INSTANCE == null) throw new IllegalStateException("ImageDecodeBackend not installed");
        return INSTANCE;
    }
}
