package org.lwjgl.util.freetype;

/** Stub FT_Face struct. */
public final class FT_Face {
    private final long handle;
    public FT_Face(long handle) { this.handle = handle; }
    public long address()     { return handle; }
    public long face_flags()  { return 0L; }
    public int  num_glyphs()  { return 0; }
    public int  units_per_EM(){ return 1000; }
    public FT_GlyphSlot glyph() { return new FT_GlyphSlot(); }
    public int ascender()     { return 0; }
    public int descender()    { return 0; }
    public int height()       { return 0; }

    public static FT_Face create(long ptr) { return new FT_Face(ptr); }
}
