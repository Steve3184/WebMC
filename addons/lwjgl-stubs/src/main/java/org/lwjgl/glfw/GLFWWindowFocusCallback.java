package org.lwjgl.glfw;
public abstract class GLFWWindowFocusCallback implements GLFWWindowFocusCallbackI {
    public static GLFWWindowFocusCallback create(GLFWWindowFocusCallbackI l) { return new GLFWWindowFocusCallback() { @Override public void invoke(long w, boolean f) { l.invoke(w, f); } }; }
    public void free() {}
    public void close() {}
}
