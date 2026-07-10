package top.steve3184.webmc.teavm.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Backend interface for image decoding.
 * Implemented by teavm-runtime module.
 */
public interface ImageDecodeBackend {
    ByteBuffer decode(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels, int desiredChannels);
    boolean info(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels);
    void setFlipVertically(boolean flip);
}
