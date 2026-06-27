package org.lwjgl.glfw;
public abstract class GLFWMouseButtonCallback implements GLFWMouseButtonCallbackI {
    public static GLFWMouseButtonCallback create(GLFWMouseButtonCallbackI l) { return new GLFWMouseButtonCallback() { @Override public void invoke(long w, int b, int a, int m) { l.invoke(w, b, a, m); } }; }
    public void free() {}
    public GLFWMouseButtonCallback set(long window) { GLFW.glfwSetMouseButtonCallback(window, this); return this; }
}
