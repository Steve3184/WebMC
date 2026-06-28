package org.lwjgl.openal;

import java.nio.IntBuffer;

/**
 * Web Audio implementation of OpenAL ALC11 for TeaVM web runtime.
 */
public final class ALC11 {
    public static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;
    public static final int ALC_MONO_SOURCES = 0x1010;
    public static final int ALC_STEREO_SOURCES = 0x1011;
    public static final int ALC_CONNECTED = 0x313;

    public static int alcGetInteger(long device, int param) {
        return ALC10.alcGetInteger(device, param);
    }

    public static void alcGetIntegerv(long device, int param, IntBuffer out) {
        ALC10.alcGetIntegerv(device, param, out);
    }

    public static void alcGetIntegerv(long device, int param, int[] out) {
        ALC10.alcGetIntegerv(device, param, out);
    }

    public static String alcGetString(long device, int param) {
        return ALC10.alcGetString(device, param);
    }

    private ALC11() {}
}