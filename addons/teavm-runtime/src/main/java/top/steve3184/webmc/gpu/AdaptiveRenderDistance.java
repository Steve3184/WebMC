package top.steve3184.webmc.gpu;

import top.steve3184.webmc.teavm.gl.GpuDetector;

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

        log("[mc-web/adaptive] Initial: renderDistance=" + currentRenderDistance
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
            // FPS in acceptable range, reset counters
            consecutiveLowFps = 0;
            consecutiveHighFps = 0;
        }
    }

    private static void decreaseQuality() {
        // Reduce render distance first (biggest impact)
        if (currentRenderDistance > 4) {
            currentRenderDistance -= 2;
            log("[mc-web/adaptive] Decreased render distance to " + currentRenderDistance);
            return;
        }

        // Then reduce particles
        if (currentParticleRange > 20) {
            currentParticleRange -= 25;
            log("[mc-web/adaptive] Decreased particle range to " + currentParticleRange);
            return;
        }

        // Disable smooth lighting
        if (smoothLighting) {
            smoothLighting = false;
            log("[mc-web/adaptive] Disabled smooth lighting");
            return;
        }

        // Disable fancy graphics
        if (fancyGraphics) {
            fancyGraphics = false;
            log("[mc-web/adaptive] Disabled fancy graphics");
        }
    }

    private static void increaseQuality() {
        GpuProfile profile = GpuDetector.getProfile();
        int maxRenderDist = profile.getRenderDistance().chunks;

        // Enable fancy graphics first
        if (!fancyGraphics) {
            fancyGraphics = true;
            log("[mc-web/adaptive] Enabled fancy graphics");
            return;
        }

        // Enable smooth lighting
        if (!smoothLighting) {
            smoothLighting = true;
            log("[mc-web/adaptive] Enabled smooth lighting");
            return;
        }

        // Increase particles
        if (currentParticleRange < 150) {
            currentParticleRange += 25;
            log("[mc-web/adaptive] Increased particle range to " + currentParticleRange);
            return;
        }

        // Increase render distance
        if (currentRenderDistance < maxRenderDist) {
            currentRenderDistance += 2;
            log("[mc-web/adaptive] Increased render distance to " + currentRenderDistance);
        }
    }

    // Getters
    public static int getRenderDistance() { return currentRenderDistance; }
    public static int getParticleRange() { return currentParticleRange; }
    public static boolean isSmoothLighting() { return smoothLighting; }
    public static boolean isFancyGraphics() { return fancyGraphics; }

    // Setters (for manual override)
    public static void setRenderDistance(int value) {
        currentRenderDistance = Math.max(4, Math.min(32, value));
    }

    public static void setParticleRange(int value) {
        currentParticleRange = Math.max(20, Math.min(150, value));
    }

    public static void setSmoothLighting(boolean value) {
        smoothLighting = value;
    }

    public static void setFancyGraphics(boolean value) {
        fancyGraphics = value;
    }

    /**
     * Reset to defaults based on GPU profile.
     */
    public static void resetToDefaults() {
        isStable = false;
        init();
        isStable = true;
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
