package org.lwjgl.glfw;
@FunctionalInterface public interface GLFWKeyCallbackI {
    void invoke(long window, int key, int scancode, int action, int mods);
}
