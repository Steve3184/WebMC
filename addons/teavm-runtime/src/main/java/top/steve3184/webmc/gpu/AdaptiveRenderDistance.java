package top.steve3184.webmc.gpu;

/**
 * Adaptive render distance system that automatically adjusts quality
 * based on current frame rate to maintain smooth gameplay.
 */
public final class AdaptiveRenderDistance {

    // Target FPS range
    private static final int TARGET_FPS_MIN = 30;
    private static final int TARGET_FPS_MAX = 60;

    // Adjustment thresholds
    private static final int FPS_DROP_THRESHOLD = 20;
    private static final int FPS_RISE_THRESHOLD = 10;

    // Current settings
    private static int currentRenderDistance = 12;
    private static int currentParticleRange = 100;
    private static boolean smoothLighting = true;
    private static boolean fancyGraphics = true;

    // Stability tracking
    private static int consecutiveLowFps = 0;
    private static int consecutiveHighFps = 0;
    private static boolean isStable = true;
    private static long lastAdjustmentTime = 0;
    private static final long MIN_ADJUSTMENT_INTERVAL = 2000; // 2 seconds

    private AdaptiveRenderDistance() {}

    /**
     * Initialize with default values based on GPU profile.
     */
    public static void init() {
        GpuProfile profile = GpuDetector.getProfile();
        switch (profile.getTier()) {
            case LOW:
                currentRenderDistance = 6;
                currentParticleRange = 50;
                smoothLighting = false;
                fancyGraphics = false;
                break;
            case MEDIUM:
                currentRenderDistance = 8;
                currentParticleRange = 75;
                smoothLighting = true;
                fancyGraphics = false;
                break;
            case HIGH:
                currentRenderDistance = 12;
                currentParticleRange = 100;
                smoothLighting = true;
                fancyGraphics = true;
                break;
            case ULTRA:
                currentRenderDistance = 16;
                currentParticleRange = 150;
                smoothLighting = true;
                fancyGraphics = true;
                break;
        }

        System.out.println("[mc-web/adaptive] Initial settings: renderDistance=" + currentRenderDistance
            + ", particles=" + currentParticleRange
            + ", smoothLighting=" + smoothLighting
            + ", fancyGraphics=" + fancyGraphics);
    }

    /**
     * Called each frame to potentially adjust settings.
     */
    public static void update(int currentFps, float frameTime) {
        if (currentFps <= 0) return;

        // Check time since last adjustment
        long now = System.currentTimeMillis();
        if (now - lastAdjustmentTime < MIN_ADJUSTMENT_INTERVAL) {
            return;
        }

        // Check if we're below target minimum
        if (currentFps < TARGET_FPS_MIN) {
            consecutiveLowFps++;
            consecutiveHighFps = 0;

            // Only adjust after consecutive low FPS
            if (consecutiveLowFps >= 3 && isStable) {
                decreaseQuality();
                consecutiveLowFps = 0;
                lastAdjustmentTime = now;
            }
        }
        // Check if we have headroom to increase quality
        else if (currentFps > TARGET_FPS_MAX + FPS_RISE_THRESHOLD) {
            consecutiveHighFps++;
            consecutiveLowFps = 0;

            // Only increase after sustained high FPS
            if (consecutiveHighFps >= 5 && isStable) {
                increaseQuality();
                consecutiveHighFps = 0;
                lastAdjustmentTime = now;
            }
        } else {
            // FPS is in acceptable range
            consecutiveLowFps = 0;
            consecutiveHighFps = 0;
            isStable = true;
        }
    }

    /**
     * Decrease render quality to improve performance.
     */
    private static void decreaseQuality() {
        isStable = false;
        System.out.println("[mc-web/adaptive] Performance drop detected - reducing quality");

        // Strategy: reduce in order of visual impact vs performance gain
        if (fancyGraphics) {
            fancyGraphics = false;
            System.out.println("[mc-web/adaptive] Disabled fancy graphics");
            return;
        }

        if (currentParticleRange > 30) {
            currentParticleRange = Math.max(30, currentParticleRange - 25);
            System.out.println("[mc-web/adaptive] Reduced particle range to " + currentParticleRange);
            return;
        }

        if (smoothLighting) {
            smoothLighting = false;
            System.out.println("[mc-web/adaptive] Disabled smooth lighting");
            return;
        }

        if (currentRenderDistance > 4) {
            currentRenderDistance = Math.max(4, currentRenderDistance - 2);
            System.out.println("[mc-web/adaptive] Reduced render distance to " + currentRenderDistance);
            return;
        }

        System.out.println("[mc-web/adaptive] At minimum quality settings");
    }

    /**
     * Increase render quality when performance allows.
     */
    private static void increaseQuality() {
        isStable = true;
        System.out.println("[mc-web/adaptive] Headroom available - increasing quality");

        // Strategy: enable in reverse order of disable
        if (currentRenderDistance < 16) {
            currentRenderDistance = Math.min(16, currentRenderDistance + 2);
            System.out.println("[mc-web/adaptive] Increased render distance to " + currentRenderDistance);
            return;
        }

        if (!smoothLighting) {
            smoothLighting = true;
            System.out.println("[mc-web/adaptive] Enabled smooth lighting");
            return;
        }

        if (currentParticleRange < 150) {
            currentParticleRange = Math.min(150, currentParticleRange + 25);
            System.out.println("[mc-web/adaptive] Increased particle range to " + currentParticleRange);
            return;
        }

        if (!fancyGraphics) {
            fancyGraphics = true;
            System.out.println("[mc-web/adaptive] Enabled fancy graphics");
            return;
        }

        System.out.println("[mc-web/adaptive] At maximum quality settings");
    }

    /**
     * Manually set render distance.
     */
    public static void setRenderDistance(int distance) {
        currentRenderDistance = Math.max(4, Math.min(32, distance));
        System.out.println("[mc-web/adaptive] Manual render distance set to " + currentRenderDistance);
    }

    /**
     * Manually set particle range.
     */
    public static void setParticleRange(int range) {
        currentParticleRange = Math.max(0, Math.min(250, range));
    }

    /**
     * Enable/disable smooth lighting.
     */
    public static void setSmoothLighting(boolean enabled) {
        smoothLighting = enabled;
    }

    /**
     * Enable/disable fancy graphics.
     */
    public static void setFancyGraphics(boolean enabled) {
        fancyGraphics = enabled;
    }

    /**
     * Force maximum quality settings.
     */
    public static void setMaxQuality() {
        currentRenderDistance = 16;
        currentParticleRange = 150;
        smoothLighting = true;
        fancyGraphics = true;
        isStable = true;
        consecutiveLowFps = 0;
        consecutiveHighFps = 0;
        System.out.println("[mc-web/adaptive] Set to maximum quality");
    }

    /**
     * Force minimum quality settings.
     */
    public static void setMinQuality() {
        currentRenderDistance = 4;
        currentParticleRange = 30;
        smoothLighting = false;
        fancyGraphics = false;
        isStable = true;
        consecutiveLowFps = 0;
        consecutiveHighFps = 0;
        System.out.println("[mc-web/adaptive] Set to minimum quality");
    }

    // Getters
    public static int getRenderDistance() { return currentRenderDistance; }
    public static int getParticleRange() { return currentParticleRange; }
    public static boolean isSmoothLighting() { return smoothLighting; }
    public static boolean isFancyGraphics() { return fancyGraphics; }
    public static boolean isStable() { return isStable; }

    /**
     * Get current settings as a diagnostic string.
     */
    public static String getSettingsString() {
        return String.format(
            "AdaptiveRenderDistance{renderDistance=%d, particles=%d, smoothLighting=%s, fancyGraphics=%s, stable=%s}",
            currentRenderDistance, currentParticleRange, smoothLighting, fancyGraphics, isStable
        );
    }
}
