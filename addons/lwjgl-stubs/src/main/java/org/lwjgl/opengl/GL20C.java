package org.lwjgl.opengl;

/** GL20C — Core profile alias of GL20 (shaders/programs). */
public final class GL20C {
    public static final int GL_VERTEX_SHADER   = GL20.GL_VERTEX_SHADER;
    public static final int GL_FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
    public static final int GL_LINK_STATUS     = GL20.GL_LINK_STATUS;
    public static final int GL_COMPILE_STATUS  = GL20.GL_COMPILE_STATUS;

    public static int  glCreateShader(int type)              { return GL20.glCreateShader(type); }
    public static void glShaderSource(int s, CharSequence src){ GL20.glShaderSource(s, src); }
    public static void glCompileShader(int s)                { GL20.glCompileShader(s); }
    public static int  glCreateProgram()                     { return GL20.glCreateProgram(); }
    public static void glAttachShader(int p, int s)          { GL20.glAttachShader(p, s); }
    public static void glLinkProgram(int p)                  { GL20.glLinkProgram(p); }
    public static void glUseProgram(int p)                   { GL20.glUseProgram(p); }
    public static int  glGetProgrami(int p, int n)           { return GL20.glGetProgrami(p, n); }
    public static int  glGetShaderi(int s, int n)            { return GL20.glGetShaderi(s, n); }
    public static String glGetProgramInfoLog(int p)          { return GL20.glGetProgramInfoLog(p); }
    public static String glGetShaderInfoLog(int s)           { return GL20.glGetShaderInfoLog(s); }

    /** Native form of glShaderSource (count, strings ptr, lengths ptr). No-op stub. */
    public static void nglShaderSource(int shader, int count, long stringsPtr, long lengthsPtr) { /* no-op */ }

    private GL20C() {}
}
