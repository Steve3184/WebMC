package org.lwjgl.util.tinyfd;

/** Stub of tinyfd. MC uses tinyfd_messageBox for crash dialogs; in browser → console.error or alert.
 *  GameNarrator uses {@code !tinyfd_messageBox(...)} so we return boolean (LWJGL actually returns int but MC casts/checks). */
public final class TinyFileDialogs {
    public static boolean tinyfd_messageBox(CharSequence title, CharSequence message, CharSequence dialogType,
                                            CharSequence iconType, boolean defaultBool) {
        System.err.println("[tinyfd stub] " + title + ": " + message);
        return false;
    }
    public static boolean tinyfd_messageBox(CharSequence title, CharSequence message, CharSequence dialogType,
                                            CharSequence iconType, int defaultButton) {
        return tinyfd_messageBox(title, message, dialogType, iconType, defaultButton != 0);
    }
    public static CharSequence tinyfd_inputBox(CharSequence title, CharSequence message, CharSequence defaultInput) { return defaultInput; }
    public static CharSequence tinyfd_saveFileDialog(CharSequence title, CharSequence defaultPath, CharSequence[] filterPatterns, CharSequence singleFilterDescription) { return null; }
    public static CharSequence tinyfd_openFileDialog(CharSequence title, CharSequence defaultPath, CharSequence[] filterPatterns, CharSequence singleFilterDescription, boolean allowMultiSelect) { return null; }
    public static CharSequence tinyfd_selectFolderDialog(CharSequence title, CharSequence defaultPath) { return null; }
    private TinyFileDialogs() {}
}
