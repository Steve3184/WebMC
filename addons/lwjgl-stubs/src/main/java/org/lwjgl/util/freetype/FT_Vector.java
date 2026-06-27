package org.lwjgl.util.freetype;

/** Stub FT_Vector struct (26.6 fixed-point pair). */
public final class FT_Vector {
    public long x;
    public long y;
    public long x()    { return x; }
    public long y()    { return y; }
    public FT_Vector x(long v) { this.x = v; return this; }
    public FT_Vector y(long v) { this.y = v; return this; }
    public FT_Vector set(long x, long y) { this.x = x; this.y = y; return this; }
    public static FT_Vector malloc(org.lwjgl.system.MemoryStack stack) { return new FT_Vector(); }
    public static FT_Vector calloc(org.lwjgl.system.MemoryStack stack) { return new FT_Vector(); }
}
