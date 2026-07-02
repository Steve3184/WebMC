package org.lwjgl.glfw;

/** Stub of {@code org.lwjgl.glfw.Callbacks}. Utility class for cleanup of native callbacks. */
public final class Callbacks {
    public static void glfwFreeCallbacks(long window) { /* no-op in browser stub */ }
    public static void glfwInvoke(long window, GLFWErrorCallbackI cb, int errCode, long descPtr) {
        if (cb != null) cb.invoke(errCode, descPtr);
    }
    private Callbacks() {}
}
