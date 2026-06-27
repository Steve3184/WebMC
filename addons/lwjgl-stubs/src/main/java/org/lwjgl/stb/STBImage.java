package org.lwjgl.stb;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Stub of {@code org.lwjgl.stb.STBImage}. Real impl decodes PNG/JPG/etc. from
 * a memory buffer. In the browser we hand off to {@code createImageBitmap}
 * via teavm-runtime; this stub holds the contract.
 *
 * NOTE: STB returns an opaque ByteBuffer that callers later free via
 * {@link #stbi_image_free}. We keep the same contract; the underlying memory
 * is JS-managed.
 */
public final class STBImage {

    public static final int STBI_default     = 0;
    public static final int STBI_grey        = 1;
    public static final int STBI_grey_alpha  = 2;
    public static final int STBI_rgb         = 3;
    public static final int STBI_rgb_alpha   = 4;

    /** Decode an in-memory image. Caller passes pre-allocated 1-element IntBuffers for w,h,channels. */
    public static ByteBuffer stbi_load_from_memory(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels, int desiredChannels) {
        return top.steve3184.webmc.teavm.io.ImageDecodeBackendHolder.current()
                .decode(encoded, w, h, channels, desiredChannels);
    }

    public static void stbi_image_free(ByteBuffer pixels) { /* GC-tracked; no-op. */ }
    public static void nstbi_image_free(long pixelsPtr)   { /* no-op */ }

    public static String stbi_failure_reason() { return null; }

    public static boolean stbi_info_from_memory(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels) {
        return top.steve3184.webmc.teavm.io.ImageDecodeBackendHolder.current()
                .info(encoded, w, h, channels);
    }

    public static void stbi_set_flip_vertically_on_load(boolean flip) {
        top.steve3184.webmc.teavm.io.ImageDecodeBackendHolder.current().setFlipVertically(flip);
    }

    private STBImage() {}
}
