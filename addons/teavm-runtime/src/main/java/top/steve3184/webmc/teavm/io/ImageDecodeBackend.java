package top.steve3184.webmc.teavm.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface ImageDecodeBackend {
    ByteBuffer decode(ByteBuffer encoded, IntBuffer width, IntBuffer height, IntBuffer channels, int desiredChannels);

    boolean info(ByteBuffer encoded, IntBuffer width, IntBuffer height, IntBuffer channels);

    void setFlipVertically(boolean flip);
}
