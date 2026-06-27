package org.lwjgl.opengl;

@FunctionalInterface
public interface GLDebugMessageARBCallbackI {
    void invoke(int source, int type, int id, int severity, int length, long message, long userParam);
}
