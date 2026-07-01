package org.lwjgl.glfw.callbacks;

public abstract class GLFWMouseButtonCallback {
    public abstract void invoke(long window, int button, int action, int mods);
}
