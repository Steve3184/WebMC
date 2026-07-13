package top.steve3184.webmc.gpu;

/**
 * GPU capability detection and adaptive rendering configuration.
 * Detects hardware capabilities and configures rendering for optimal performance.
 */
public final class GpuProfile {

    /** GPU tier based on detected capabilities */
    public enum Tier {
        /** Integrated/entry-level GPU - minimal features */
        LOW(0, "Low", 4),
        /** Mid-range GPU - balanced features */
        MEDIUM(1, "Medium", 8),
        /** High-end/discrete GPU - all features */
        HIGH(2, "High", 16),
        /** Workstation/benchmark GPU - maximum features */
        ULTRA(3, "Ultra", 32);

        public final int level;
        public final String name;
        public final int maxTextureUnits;

        Tier(int level, String name, int maxTextureUnits) {
            this.level = level;
            this.name = name;
            this.maxTextureUnits = maxTextureUnits;
        }
    }

    /** Render distance levels */
    public enum RenderDistance {
        TINY(4, 48),
        SHORT(8, 96),
        NORMAL(12, 192),
        FAR(16, 256),
        EXTREME(32, 512);

        public final int chunks;
        public final int viewDistance;

        RenderDistance(int chunks, int viewDistance) {
            this.chunks = chunks;
            this.viewDistance = viewDistance;
        }
    }

    private final Tier tier;
    private final RenderDistance renderDistance;
    private final boolean hasWebGL2;
    private final int maxTextureSize;
    private final int maxVertexAttribs;
    private final boolean supportsFloatTextures;
    private final boolean supportsDepthTexture;
    private final boolean supportsInstancing;
    private final boolean supportsMipmaps;

    private GpuProfile(Builder builder) {
        this.tier = builder.tier;
        this.renderDistance = builder.renderDistance;
        this.hasWebGL2 = builder.hasWebGL2;
        this.maxTextureSize = builder.maxTextureSize;
        this.maxVertexAttribs = builder.maxVertexAttribs;
        this.supportsFloatTextures = builder.supportsFloatTextures;
        this.supportsDepthTexture = builder.supportsDepthTexture;
        this.supportsInstancing = builder.supportsInstancing;
        this.supportsMipmaps = builder.supportsMipmaps;
    }

    // Getters
    public Tier getTier() { return tier; }
    public RenderDistance getRenderDistance() { return renderDistance; }
    public boolean hasWebGL2() { return hasWebGL2; }
    public int getMaxTextureSize() { return maxTextureSize; }
    public int getMaxVertexAttribs() { return maxVertexAttribs; }
    public boolean supportsFloatTextures() { return supportsFloatTextures; }
    public boolean supportsDepthTexture() { return supportsDepthTexture; }
    public boolean supportsInstancing() { return supportsInstancing; }
    public boolean supportsMipmaps() { return supportsMipmaps; }

    // Performance hints
    public boolean shouldUseBatching() { return tier.level >= Tier.MEDIUM.level; }
    public boolean shouldUseShadows() { return tier.level >= Tier.MEDIUM.level; }
    public boolean shouldUseSmoothLighting() { return tier.level >= Tier.MEDIUM.level; }
    public boolean shouldUseFancyGraphics() { return tier.level >= Tier.HIGH.level; }
    public boolean shouldUseAntialiasing() { return tier.level >= Tier.HIGH.level; }
    public boolean shouldUseVsync() { return true; } // Always on for smoothness
    public boolean shouldUseDynamicFps() { return tier.level < Tier.HIGH.level; }

    // Texture size recommendations based on GPU tier
    public int getRecommendedTextureSize() {
        switch (tier) {
            case LOW:    return 256;
            case MEDIUM: return 512;
            case HIGH:   return 1024;
            case ULTRA:  return 2048;
            default:     return 512;
        }
    }

    // Chunk buffer size recommendations
    public int getChunkBufferSize() {
        switch (tier) {
            case LOW:    return 64 * 1024;      // 64KB
            case MEDIUM: return 256 * 1024;     // 256KB
            case HIGH:   return 1024 * 1024;    // 1MB
            case ULTRA:  return 4 * 1024 * 1024; // 4MB
            default:     return 256 * 1024;
        }
    }

    // Particle count limits
    public int getMaxParticles() {
        switch (tier) {
            case LOW:    return 1000;
            case MEDIUM: return 5000;
            case HIGH:   return 10000;
            case ULTRA:  return 50000;
            default:     return 5000;
        }
    }

    // Entity render distance multiplier
    public float getEntityRenderDistance() {
        switch (tier) {
            case LOW:    return 0.5f;
            case MEDIUM: return 0.75f;
            case HIGH:   return 1.0f;
            case ULTRA:  return 1.5f;
            default:     return 0.75f;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "GpuProfile{tier=%s, renderDistance=%s, webgl2=%s, maxTexture=%d, instancing=%s}",
            tier.name, renderDistance.name(), hasWebGL2, maxTextureSize, supportsInstancing
        );
    }

    public static class Builder {
        private Tier tier = Tier.MEDIUM;
        private RenderDistance renderDistance = RenderDistance.NORMAL;
        private boolean hasWebGL2 = true;
        private int maxTextureSize = 2048;
        private int maxVertexAttribs = 16;
        private boolean supportsFloatTextures = false;
        private boolean supportsDepthTexture = true;
        private boolean supportsInstancing = false;
        private boolean supportsMipmaps = true;

        public Builder setTier(Tier tier) { this.tier = tier; return this; }
        public Builder setRenderDistance(RenderDistance rd) { this.renderDistance = rd; return this; }
        public Builder setHasWebGL2(boolean v) { this.hasWebGL2 = v; return this; }
        public Builder setMaxTextureSize(int v) { this.maxTextureSize = v; return this; }
        public Builder setMaxVertexAttribs(int v) { this.maxVertexAttribs = v; return this; }
        public Builder setSupportsFloatTextures(boolean v) { this.supportsFloatTextures = v; return this; }
        public Builder setSupportsDepthTexture(boolean v) { this.supportsDepthTexture = v; return this; }
        public Builder setSupportsInstancing(boolean v) { this.supportsInstancing = v; return this; }
        public Builder setSupportsMipmaps(boolean v) { this.supportsMipmaps = v; return this; }

        /**
         * Auto-detect tier from GPU renderer string.
         */
        public Builder inferTierFromRenderer(String renderer) {
            if (renderer == null) {
                this.tier = Tier.MEDIUM;
                return this;
            }

            String r = renderer.toLowerCase();

            // Low-end indicators
            if (r.contains("intel") && !r.contains("iris") && !r.contains("arc") ||
                r.contains("mesa") || r.contains("llvmpipe") ||
                r.contains("swiftshader") || r.contains("software")) {
                this.tier = Tier.LOW;
            }
            // Mid-range
            else if (r.contains("intel iris") || r.contains("intel arc") ||
                     r.contains("adreno") || r.contains("mali") ||
                     r.contains("powervr") || r.contains("apple gpu")) {
                this.tier = Tier.MEDIUM;
            }
            // High-end
            else if (r.contains("nvidia") && !r.contains("geforce gt") ||
                     r.contains("radeon rx") || r.contains("radeon pro") ||
                     r.contains("arc a")) {
                this.tier = Tier.HIGH;
            }
            // Workstation
            else if (r.contains("nvidia quadro") || r.contains("nvidia tesla") ||
                     r.contains("radeon pro wx") || r.contains("apple m")) {
                this.tier = Tier.ULTRA;
            }
            else {
                this.tier = Tier.MEDIUM;
            }

            return this;
        }

        public GpuProfile build() {
            return new GpuProfile(this);
        }
    }
}
