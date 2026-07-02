package org.lwjgl.glfw;

@FunctionalInterface
public interface GLFWCharModsCallbackI {
    void invoke(long window, int codepoint, int mods);
}
