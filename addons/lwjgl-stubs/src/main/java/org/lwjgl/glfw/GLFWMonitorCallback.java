package org.lwjgl.glfw;
public abstract class GLFWMonitorCallback implements GLFWMonitorCallbackI {
    public static GLFWMonitorCallback create(GLFWMonitorCallbackI l) {
        return new GLFWMonitorCallback() { @Override public void invoke(long m, int e) { l.invoke(m, e); } };
    }
    public void free()  {}
    public void close() {}
}
