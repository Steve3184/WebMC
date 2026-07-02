package org.lwjgl.opengl;

/** GL32C — Core alias of GL32 (sync). */
public final class GL32C {
    public static final long GL_TIMEOUT_IGNORED       = GL32.GL_TIMEOUT_IGNORED;
    public static final int  GL_SYNC_GPU_COMMANDS_COMPLETE = GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
    public static final int  GL_ALREADY_SIGNALED      = GL32.GL_ALREADY_SIGNALED;
    public static final int  GL_TIMEOUT_EXPIRED       = GL32.GL_TIMEOUT_EXPIRED;
    public static final int  GL_CONDITION_SATISFIED   = GL32.GL_CONDITION_SATISFIED;
    public static final int  GL_WAIT_FAILED           = GL32.GL_WAIT_FAILED;
    public static final int  GL_SYNC_FLUSH_COMMANDS_BIT = GL32.GL_SYNC_FLUSH_COMMANDS_BIT;

    public static long glFenceSync(int condition, int flags)      { return GL32.glFenceSync(condition, flags); }
    public static int  glClientWaitSync(long s, int f, long t)    { return GL32.glClientWaitSync(s, f, t); }
    public static void glDeleteSync(long s)                       { GL32.glDeleteSync(s); }

    // Timer query subset (used by Blaze3D TimerQuery class)
    public static int  glGenQueries()                             { return 0; }
    public static void glGenQueries(java.nio.IntBuffer out)       { }
    public static void glBeginQuery(int target, int id)           { }
    public static void glEndQuery(int target)                     { }
    public static void glDeleteQueries(int id)                    { }
    public static void glDeleteQueries(java.nio.IntBuffer ids)    { }
    public static int  glGetQueryObjecti(int id, int pname)       { return 0; }
    public static int  glGetQueryi(int target, int pname)         { return 0; }

    private GL32C() {}
}
