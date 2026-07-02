package javax.imageio;

public class ImageIO {
    private ImageIO() {}
    public static java.awt.image.BufferedImage read(java.io.InputStream input) { return null; }
    public static java.awt.image.BufferedImage read(java.io.File input) { return null; }
    public static java.awt.image.BufferedImage read(java.net.URL input) { return null; }
    public static boolean write(java.awt.image.RenderedImage im, String formatName, java.io.OutputStream output) { return false; }
    public static boolean write(java.awt.image.RenderedImage im, String formatName, java.io.File output) { return false; }
    public static String[] getReaderFormatNames() { return new String[0]; }
    public static String[] getWriterFormatNames() { return new String[0]; }
}
