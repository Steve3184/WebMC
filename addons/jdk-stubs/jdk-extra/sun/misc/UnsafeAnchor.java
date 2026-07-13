package sun.misc;

/**
 * TeaVM requires explicit references to preserve classes during compilation.
 * This class acts as an anchor to ensure sun.misc.Unsafe is included in the output.
 * Without this, TeaVM's reachability analysis would exclude Unsafe, causing
 * "Class sun.misc.Unsafe was not found" at runtime when Minecraft uses reflection.
 */
public class UnsafeAnchor {
    // Force inclusion of Unsafe by creating a static field of that type
    private static final Unsafe UNSAFE_ANCHOR_FIELD = null;

    // Called at startup to ensure the class is loaded and preserved
    public static void ensureLoaded() {
        // Access the field to prevent dead-code elimination
        if (UNSAFE_ANCHOR_FIELD != null) {
            throw new IllegalStateException("Unsafe should not be instantiated");
        }
    }
}
