package org.lwjgl.openal;

/**
 * Web Audio ALC Capabilities for TeaVM web runtime.
 */
public final class ALCCapabilities {
    public final boolean OpenALC10 = true;
    public final boolean OpenALC11 = true;
    public final boolean ALC_EXT_disconnect = true;
    public final boolean ALC_EXT_thread_local_context = false;
    public final boolean ALC_ENUMERATE_ALL_EXT = true;
    public final boolean ALC_SOFT_HRTF = false;
    public final boolean ALC_SOFT_pause_device = false;
    public final boolean ALC_SOFT_output_limiter = false;
}