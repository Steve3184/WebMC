package org.lwjgl.glfw;
public abstract class GLFWErrorCallback implements GLFWErrorCallbackI {
    public static GLFWErrorCallback create(GLFWErrorCallbackI lambda) {
        return new GLFWErrorCallback() { @Override public void invoke(int error, long description) { lambda.invoke(error, description); } };
    }
    public static GLFWErrorCallback createPrint(java.io.PrintStream out) {
        return new GLFWErrorCallback() {
            @Override public void invoke(int error, long description) { out.println("GLFW error " + error); }
        };
    }
    public void free()  {}
    public void close() {}
    public GLFWErrorCallback set() { GLFW.glfwSetErrorCallback(this); return this; }
}
