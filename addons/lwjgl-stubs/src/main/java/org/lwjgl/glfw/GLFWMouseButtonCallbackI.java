package org.lwjgl.glfw;
@FunctionalInterface public interface GLFWMouseButtonCallbackI {
    void invoke(long window, int button, int action, int mods);
}
