package org.lwjgl.glfw;
public abstract class GLFWCharCallback implements GLFWCharCallbackI {
    public static GLFWCharCallback create(GLFWCharCallbackI l) { return new GLFWCharCallback() { @Override public void invoke(long w, int c) { l.invoke(w, c); } }; }
    public void free() {}
    public GLFWCharCallback set(long window) { GLFW.glfwSetCharCallback(window, this); return this; }
}
