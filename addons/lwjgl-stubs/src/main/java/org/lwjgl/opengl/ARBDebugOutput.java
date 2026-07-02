package org.lwjgl.opengl;

/** Stub of ARB_debug_output. Older variant of KHR_debug; same idea. */
public final class ARBDebugOutput {
    public static final int GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB = 0x8242;
    public static void glDebugMessageControlARB(int s, int t, int sev, int[] ids, boolean enabled) {}
    public static void glDebugMessageCallbackARB(GLDebugMessageARBCallback cb, long userParam) {}
    private ARBDebugOutput() {}
}
