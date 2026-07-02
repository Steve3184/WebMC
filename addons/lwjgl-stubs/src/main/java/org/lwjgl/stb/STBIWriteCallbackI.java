package org.lwjgl.stb;

@FunctionalInterface
public interface STBIWriteCallbackI {
    void invoke(long context, long data, int size);
}
