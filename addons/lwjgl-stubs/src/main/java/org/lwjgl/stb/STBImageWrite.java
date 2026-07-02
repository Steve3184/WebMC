package org.lwjgl.stb;

import java.nio.ByteBuffer;

/** Stub of stb_image_write. PNG/JPG writing — used for screenshots. Phase 5: HTMLCanvas.toBlob. */
public final class STBImageWrite {
    public static boolean stbi_write_png(CharSequence filename, int w, int h, int comp, ByteBuffer data, int strideInBytes) { return false; }
    public static boolean stbi_write_png_to_func(STBIWriteCallbackI cb, long context, int w, int h, int comp, ByteBuffer data, int strideInBytes) { return false; }
    public static boolean stbi_write_png_to_func(STBIWriteCallback cb, long context, int w, int h, int comp, ByteBuffer data, int strideInBytes) { return false; }
    public static boolean stbi_write_jpg(CharSequence filename, int w, int h, int comp, ByteBuffer data, int quality) { return false; }
    public static boolean stbi_write_bmp(CharSequence filename, int w, int h, int comp, ByteBuffer data) { return false; }
    public static int nstbi_write_png_to_func(long callbackAddr, long contextPtr, int w, int h, int comp, java.nio.ByteBuffer pixels, int strideInBytes) { return 0; }
    public static int nstbi_write_png_to_func(long callbackAddr, long contextPtr, int w, int h, int comp, long pixelsPtr, int strideInBytes) { return 0; }
    private STBImageWrite() {}
}
