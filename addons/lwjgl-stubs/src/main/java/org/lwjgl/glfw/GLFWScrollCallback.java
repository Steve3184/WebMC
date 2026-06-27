package org.lwjgl.glfw;
public abstract class GLFWScrollCallback implements GLFWScrollCallbackI {
    public static GLFWScrollCallback create(GLFWScrollCallbackI l) { return new GLFWScrollCallback() { @Override public void invoke(long w, double x, double y) { l.invoke(w, x, y); } }; }
    public void free() {}
    public GLFWScrollCallback set(long window) { GLFW.glfwSetScrollCallback(window, this); return this; }
}
