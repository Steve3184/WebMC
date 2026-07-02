package org.lwjgl.opengl;

/** Stub of {@code org.lwjgl.opengl.GL33}. Sampler objects, vertex attrib divisor. */
public final class GL33 {
    public static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    public static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    public static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    public static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    public static final int GL_TEXTURE_SWIZZLE_RGBA = 0x8E46;

    public static void glVertexAttribDivisor(int index, int divisor) {
        top.steve3184.webmc.teavm.gl.GLBackendHolder.current().vertexAttribDivisor(index, divisor);
    }
    private GL33() {}
}
