package org.lwjgl.util.freetype;

import java.nio.ByteBuffer;

/** Stub of FreeType library entry. Phase 5: replace with browser font rendering. */
public final class FreeType {
    public static final int FT_LOAD_DEFAULT  = 0;
    public static final int FT_LOAD_RENDER   = 0x4;
    public static final int FT_LOAD_NO_HINTING = 0x2;
    public static final int FT_LOAD_NO_BITMAP = 0x8;
    public static final int FT_LOAD_FORCE_AUTOHINT = 0x20;
    public static final int FT_LOAD_TARGET_MONO = 0x20000;
    public static final int FT_LOAD_TARGET_NORMAL = 0;
    public static final int FT_PIXEL_MODE_MONO = 1;
    public static final int FT_PIXEL_MODE_GRAY = 2;
    public static final int FT_KERNING_DEFAULT = 0;
    public static final int FT_RENDER_MODE_NORMAL = 0;
    public static final int FT_RENDER_MODE_MONO = 2;
    public static final int FT_FACE_FLAG_KERNING = 0x40;

    public static final int FT_ENCODING_UNICODE   = 0x756E6963; // 'unic'
    public static final int FT_ENCODING_NONE      = 0;

    public static int FT_Init_FreeType(java.nio.LongBuffer libraryOut)            { return 0; }
    public static int FT_Init_FreeType(org.lwjgl.PointerBuffer libraryOut)        { return 0; }
    public static int FT_Done_FreeType(long library)                              { return 0; }
    public static int FT_Done_Library(long library)                               { return 0; }
    public static int FT_New_Memory_Face(long library, ByteBuffer fontData, long faceIndex, java.nio.LongBuffer faceOut) { return 0; }
    public static int FT_New_Memory_Face(long library, ByteBuffer fontData, long faceIndex, org.lwjgl.PointerBuffer faceOut) { return 0; }
    public static int FT_Done_Face(long face)                                     { return 0; }
    public static int FT_Done_Face(FT_Face face)                                  { return 0; }
    public static int FT_Set_Pixel_Sizes(long face, int w, int h)                 { return 0; }
    public static int FT_Set_Pixel_Sizes(FT_Face face, int w, int h)              { return 0; }
    public static int FT_Set_Char_Size(long face, long w, long h, int hRes, int vRes) { return 0; }
    public static int FT_Set_Char_Size(FT_Face face, long w, long h, int hRes, int vRes) { return 0; }
    public static int FT_Get_Char_Index(long face, long charCode)                 { return 0; }
    public static int FT_Get_Char_Index(FT_Face face, long charCode)              { return 0; }
    public static long FT_Get_First_Char(FT_Face face, java.nio.IntBuffer agindex){ return 0L; }
    public static long FT_Get_First_Char(long face, java.nio.IntBuffer agindex)   { return 0L; }
    public static long FT_Get_Next_Char(FT_Face face, long charCode, java.nio.IntBuffer agindex) { return 0L; }
    public static long FT_Get_Next_Char(long face, long charCode, java.nio.IntBuffer agindex)    { return 0L; }
    public static int FT_Load_Glyph(long face, int glyphIndex, int loadFlags)     { return 0; }
    public static int FT_Load_Glyph(FT_Face face, int glyphIndex, int loadFlags)  { return 0; }
    public static int FT_Render_Glyph(long glyphSlot, int renderMode)             { return 0; }
    public static int FT_Render_Glyph(FT_GlyphSlot glyphSlot, int renderMode)     { return 0; }
    public static int FT_Get_Kerning(long face, int leftGlyph, int rightGlyph, int kernMode, FT_Vector outVec) { return 0; }
    public static int FT_Get_Kerning(FT_Face face, int leftGlyph, int rightGlyph, int kernMode, FT_Vector outVec) { return 0; }
    public static int FT_Set_Transform(FT_Face face, FT_Matrix matrix, FT_Vector delta) { return 0; }
    public static int FT_Set_Transform(long face, long matrix, FT_Vector delta)        { return 0; }
    public static int FT_Select_Charmap(FT_Face face, int encoding)                { return 0; }
    public static int FT_Select_Charmap(long face, int encoding)                   { return 0; }
    public static String FT_Get_Font_Format(FT_Face face)                          { return "TrueType"; }
    public static String FT_Get_Font_Format(long face)                             { return "TrueType"; }

    public static String FT_Error_String(int errorCode) { return "FreeType-stub-error-" + errorCode; }

    private FreeType() {}
}
