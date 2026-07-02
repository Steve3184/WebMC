package org.lwjgl.opengl;

/** Stub of EXT_debug_label. Pure debug — no-op everywhere. */
public final class EXTDebugLabel {
    public static final int GL_BUFFER_OBJECT_EXT  = 0x9151;
    public static final int GL_PROGRAM_OBJECT_EXT = 0x8B40;
    public static void glLabelObjectEXT(int type, int id, CharSequence label) {}
    private EXTDebugLabel() {}
}
