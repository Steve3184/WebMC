package org.lwjgl.glfw;

@FunctionalInterface
public interface GLFWDropCallbackI {
    void invoke(long window, int count, long names);
}
