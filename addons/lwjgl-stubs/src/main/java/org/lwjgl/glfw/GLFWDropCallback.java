package org.lwjgl.glfw;

public abstract class GLFWDropCallback implements GLFWDropCallbackI {
    public static GLFWDropCallback create(GLFWDropCallbackI l) {
        return new GLFWDropCallback() { @Override public void invoke(long w, int c, long n) { l.invoke(w, c, n); } };
    }
    public void free() {}
    public GLFWDropCallback set(long window) { return this; /* no-op */ }
    public static String getName(long names, int index) { return ""; /* drop not supported in browser stub */ }
}
