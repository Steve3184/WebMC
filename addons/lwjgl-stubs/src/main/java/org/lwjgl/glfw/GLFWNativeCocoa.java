package org.lwjgl.glfw;

/** Stub of GLFWNativeCocoa. macOS-specific; in browser we never need it. */
public final class GLFWNativeCocoa {
    public static long glfwGetCocoaWindow(long window) { return 0L; }
    private GLFWNativeCocoa() {}
}
