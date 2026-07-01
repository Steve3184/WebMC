package org.lwjgl.glfw.callbacks;

public abstract class GLFWKeyCallback {
    public abstract void invoke(long window, int key, int scancode, int action, int mods);
}
