package org.lwjgl.glfw.callbacks;

public abstract class GLFWScrollCallback {
    public abstract void invoke(long window, double xoffset, double yoffset);
}
