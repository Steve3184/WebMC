package org.lwjgl.openal;

/**
 * Web Audio OpenAL Context entry point for TeaVM web runtime.
 */
public final class ALC {

    private static ALCCapabilities capabilities;

    public static ALCCapabilities createCapabilities(long device) {
        if (capabilities == null) {
            capabilities = new ALCCapabilities();
        }
        return capabilities;
    }

    public static void destroy() {
        capabilities = null;
    }

    private ALC() {}
}