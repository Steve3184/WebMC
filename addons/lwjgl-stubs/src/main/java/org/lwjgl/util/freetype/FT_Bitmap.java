package org.lwjgl.util.freetype;

import java.nio.ByteBuffer;

/** Stub FT_Bitmap struct. */
public final class FT_Bitmap {
    public int rows()        { return 0; }
    public int width()        { return 0; }
    public int pitch()        { return 0; }
    public int pixel_mode()   { return FreeType.FT_PIXEL_MODE_GRAY; }
    public ByteBuffer buffer(int len) { return ByteBuffer.allocate(len); }
}
