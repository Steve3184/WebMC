package org.lwjgl.opengl;

/** GL11C is the "Core profile" alias of GL11 in LWJGL — same constants and functions. */
public final class GL11C {
    public static final int GL_NO_ERROR  = GL11.GL_NO_ERROR;
    public static final int GL_TRIANGLES = GL11.GL_TRIANGLES;
    public static final int GL_LINES     = GL11.GL_LINES;
    public static final int GL_FLOAT     = GL11.GL_FLOAT;
    public static final int GL_UNSIGNED_INT = GL11.GL_UNSIGNED_INT;
    public static final int GL_UNSIGNED_BYTE = GL11.GL_UNSIGNED_BYTE;

    public static int  glGetError()                           { return GL11.glGetError(); }
    public static String glGetString(int name)                { return GL11.glGetString(name); }
    public static void glClear(int mask)                      { GL11.glClear(mask); }
    public static void glClearColor(float r, float g, float b, float a) { GL11.glClearColor(r,g,b,a); }
    public static void glViewport(int x, int y, int w, int h) { GL11.glViewport(x,y,w,h); }
    public static void glDrawArrays(int mode, int first, int count) { GL11.glDrawArrays(mode,first,count); }
    public static void glDrawElements(int mode, int count, int type, long offset) { GL11.glDrawElements(mode,count,type,offset); }
    public static void glEnable(int cap)                      { GL11.glEnable(cap); }
    public static void glDisable(int cap)                     { GL11.glDisable(cap); }
    public static void glBindTexture(int target, int id)      { GL11.glBindTexture(target, id); }
    public static int  glGenTextures()                        { return GL11.glGenTextures(); }
    public static void glDeleteTextures(int id)               { GL11.glDeleteTextures(id); }
    public static void glPixelStorei(int pname, int param)    { GL11.glPixelStorei(pname, param); }
    public static int  glGetInteger(int pname)                { return 0; }
    public static void glReadPixels(int x, int y, int w, int h, int format, int type, java.nio.ByteBuffer pixels) {
        GL11.glReadPixels(x,y,w,h,format,type,pixels);
    }

    private GL11C() {}
}
