package org.lwjgl.opengl;

/** Stub of {@code GL14}: blend constants + min/max. Constants only. */
public final class GL14 {
    public static final int GL_CONSTANT_COLOR           = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA           = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_DEPTH_COMPONENT16        = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24        = 0x81A6;
    public static final int GL_DEPTH_COMPONENT32        = 0x81A7;
    public static final int GL_INCR_WRAP                = 0x8507;
    public static final int GL_DECR_WRAP                = 0x8508;
    public static final int GL_MIRRORED_REPEAT          = 0x8370;

    public static void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA) {
        top.steve3184.webmc.teavm.gl.GLBackendHolder.current().blendFuncSeparate(srcRGB, dstRGB, srcA, dstA);
    }
    private GL14() {}
}
