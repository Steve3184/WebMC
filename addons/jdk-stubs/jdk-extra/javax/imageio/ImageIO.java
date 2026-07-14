package javax.imageio;

public class ImageIO {
    private ImageIO() {}

    // Stub: Browser cannot load images via Java ImageIO API
    public static Object read(java.io.InputStream input) {
        return null;
    }

    public static boolean write(Object imaged, String formatName, java.io.OutputStream output) {
        return false;
    }
}
