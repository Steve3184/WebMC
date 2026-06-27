package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

/** Phase-1 texture-view stub. */
public final class WebGpuTextureView extends GpuTextureView {
    private boolean closed = false;
    public WebGpuTextureView(GpuTexture tex, int baseMipLevel, int mipLevels) {
        super(tex, baseMipLevel, mipLevels);
    }
    @Override public void close()       { closed = true; }
    @Override public boolean isClosed() { return closed; }
}
