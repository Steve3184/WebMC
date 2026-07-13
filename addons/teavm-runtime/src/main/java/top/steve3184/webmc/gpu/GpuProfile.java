package top.steve3184.webmc.gpu;

/**
 * GPU profile containing detected capabilities and recommended settings.
 * Used to configure rendering quality and features based on hardware.
 */
public final class GpuProfile {

    public enum Tier {
        LOW(0),
        MEDIUM(1),
        HIGH(2),
        ULTRA(3);

        public final int level;
        Tier(int level) { this.level = level; }
    }

    public enum RenderDistance {
        TINY(4),      // 4 chunks
        SHORT(8),     // 8 chunks
        NORMAL(12),   // 12 chunks
        FAR(16),      // 16 chunks
        EXTREME(24);  // 24 chunks

        public final int chunks;
        RenderDistance(int chunks) { this.chunks = chunks; }
    }

    // Builder implementation
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean hasWebGL2 = true;
        private Tier tier = Tier.MEDIUM;
        private RenderDistance renderDistance = RenderDistance.NORMAL;
        private int maxTextureSize = 4096;
        private int maxVertexAttribs = 8;
        private boolean supportsDepthTexture = false;
        private boolean supportsFloatTextures = false;
        private boolean supportsInstancing = true;

        public Builder hasWebGL2(boolean v) { hasWebGL2 = v; return this; }
        public Builder tier(Tier v) { tier = v; return this; }
        public Builder renderDistance(RenderDistance v) { renderDistance = v; return this; }
        public Builder maxTextureSize(int v) { maxTextureSize = v; return this; }
        public Builder maxVertexAttribs(int v) { maxVertexAttribs = v; return this; }
        public Builder supportsDepthTexture(boolean v) { supportsDepthTexture = v; return this; }
        public Builder supportsFloatTextures(boolean v) { supportsFloatTextures = v; return this; }
        public Builder supportsInstancing(boolean v) { supportsInstancing = v; return this; }

        public GpuProfile build() {
            return new GpuProfile(hasWebGL2, tier, renderDistance, maxTextureSize,
                                  maxVertexAttribs, supportsDepthTexture, supportsFloatTextures,
                                  supportsInstancing);
        }
    }

    // Immutable fields
    private final boolean hasWebGL2;
    private final Tier tier;
    private final RenderDistance renderDistance;
    private final int maxTextureSize;
    private final int maxVertexAttribs;
    private final boolean supportsDepthTexture;
    private final boolean supportsFloatTextures;
    private final boolean supportsInstancing;

    private GpuProfile(boolean hasWebGL2, Tier tier, RenderDistance renderDistance,
                       int maxTextureSize, int maxVertexAttribs,
                       boolean supportsDepthTexture, boolean supportsFloatTextures,
                       boolean supportsInstancing) {
        this.hasWebGL2 = hasWebGL2;
        this.tier = tier;
        this.renderDistance = renderDistance;
        this.maxTextureSize = maxTextureSize;
        this.maxVertexAttribs = maxVertexAttribs;
        this.supportsDepthTexture = supportsDepthTexture;
        this.supportsFloatTextures = supportsFloatTextures;
        this.supportsInstancing = supportsInstancing;
    }

    // Getters
    public boolean hasWebGL2() { return hasWebGL2; }
    public Tier getTier() { return tier; }
    public RenderDistance getRenderDistance() { return renderDistance; }
    public int getMaxTextureSize() { return maxTextureSize; }
    public int getMaxVertexAttribs() { return maxVertexAttribs; }
    public boolean supportsDepthTexture() { return supportsDepthTexture; }
    public boolean supportsFloatTextures() { return supportsFloatTextures; }
    public boolean supportsInstancing() { return supportsInstancing; }

    // Quality hints based on tier
    public boolean useBiomeColors() { return tier.level >= Tier.MEDIUM.level; }
    public boolean useSmoothLighting() { return tier.level >= Tier.HIGH.level; }
    public boolean useClouds() { return tier.level >= Tier.MEDIUM.level; }
    public boolean useParticles() { return tier.level >= Tier.MEDIUM.level; }
    public boolean useWeather() { return tier.level >= Tier.HIGH.level; }

    // Render distance in blocks (chunks * 16)
    public int getRenderDistanceBlocks() {
        return renderDistance.chunks * 16;
    }

    // Texture atlas size recommendation
    public int getRecommendedAtlasSize() {
        if (tier == Tier.LOW) return 512;
        if (tier == Tier.MEDIUM) return 1024;
        if (tier == Tier.HIGH) return 2048;
        return 4096;
    }

    @Override
    public String toString() {
        return "GpuProfile{" +
               "tier=" + tier +
               ", webgl2=" + hasWebGL2 +
               ", renderDistance=" + renderDistance +
               ", maxTextureSize=" + maxTextureSize +
               ", maxVertexAttribs=" + maxVertexAttribs +
               '}';
    }
}
