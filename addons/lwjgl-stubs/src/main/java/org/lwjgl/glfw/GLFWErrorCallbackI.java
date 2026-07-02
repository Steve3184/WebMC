package org.lwjgl.glfw;

@FunctionalInterface
public interface GLFWErrorCallbackI {
    void invoke(int error, long description);
}
