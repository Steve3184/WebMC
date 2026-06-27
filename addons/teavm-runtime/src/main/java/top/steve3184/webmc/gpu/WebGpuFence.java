package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.buffers.GpuFence;

/**
 * Phase-1 fence stub. Real impl wraps {@code gl.fenceSync} / {@code clientWaitSync}.
 * Currently {@link #awaitCompletion(long)} returns true immediately — fine for
 * boot-time use; commands have effectively no async deferral on WebGL anyway.
 */
public final class WebGpuFence implements GpuFence {
    private boolean closed = false;
    @Override public void close() { closed = true; }
    @Override public boolean awaitCompletion(long timeoutNanos) { return true; }
}
