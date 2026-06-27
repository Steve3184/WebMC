package org.lwjgl.opengl;

public abstract class GLDebugMessageCallback implements GLDebugMessageCallbackI, org.lwjgl.system.Pointer {
    public void free()    {}
    public void close()   {}
    public long address() { return 0L; }
    public static String getMessage(int length, long message) { return ""; }
    public static GLDebugMessageCallback create(GLDebugMessageCallbackI lambda) {
        return new GLDebugMessageCallback() {
            @Override public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
                lambda.invoke(source, type, id, severity, length, message, userParam);
            }
        };
    }
}
