package top.steve3184.webmc.teavm.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Image decode backend (PNG/JPG → RGBA8). Implemented in teavm-runtime via createImageBitmap. */
public interface ImageDecodeBackend {
    /** Returns a direct ByteBuffer holding RGBA pixels; sets w/h/channels. */
    ByteBuffer decode(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels, int desiredChannels);
    /** Probe header only; no pixel decode. */
    boolean info(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels);
    void setFlipVertically(boolean flip);
}
