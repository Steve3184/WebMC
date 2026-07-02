package org.lwjgl.glfw;
public abstract class GLFWWindowCloseCallback implements GLFWWindowCloseCallbackI {
    public static GLFWWindowCloseCallback create(GLFWWindowCloseCallbackI l) { return new GLFWWindowCloseCallback() { @Override public void invoke(long w) { l.invoke(w); } }; }
    public void free() {}
    public void close() {}
}
