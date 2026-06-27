package org.lwjgl.opengl;

/**
 * Stub of {@code org.lwjgl.opengl.GL}. Returns a singleton {@link GLCapabilities}.
 * MC calls {@code GL.createCapabilities()} once at boot.
 */
public final class GL {
    private static final GLCapabilities CAPS = new GLCapabilities();

    public static GLCapabilities createCapabilities()                  { return CAPS; }
    public static GLCapabilities createCapabilities(boolean forwardCompat) { return CAPS; }
    public static GLCapabilities getCapabilities()                     { return CAPS; }
    public static void setCapabilities(GLCapabilities caps)            { /* no-op */ }
    private GL() {}
}
