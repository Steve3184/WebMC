package java.awt.image;

public class BufferedImage {
    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_BYTE_GRAY = 10;

    private final int width;
    private final int height;
    private final int type;
    private final int[] pixels;

    public BufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.type = imageType;
        this.pixels = new int[width * height];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getType() { return type; }
    public int getRGB(int x, int y) { return pixels[y * width + x]; }
    public void setRGB(int x, int y, int rgb) { pixels[y * width + x] = rgb; }
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        if (rgbArray == null) rgbArray = new int[offset + h * scansize];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                rgbArray[offset + y * scansize + x] = pixels[(startY + y) * width + (startX + x)];
            }
        }
        return rgbArray;
    }
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[(startY + y) * width + (startX + x)] = rgbArray[offset + y * scansize + x];
            }
        }
    }
}
