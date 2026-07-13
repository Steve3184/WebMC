package top.steve3184.webmc.teavm.gl.render;

import top.steve3184.webmc.teavm.gl.GpuDetector;

/**
 * Adaptive render distance that adjusts based on GPU performance.
 * Monitors FPS and dynamically adjusts render distance to maintain target frame rate.
 */
public final class AdaptiveRenderDistance {

    private static AdaptiveRenderDistance instance;

    private GpuDetector.GpuProfile profile;
    private int currentRenderDistance;
    private int minRenderDistance = 4;
    private int maxRenderDistance = 32;
    private int targetFPS = 30;

    // FPS monitoring
    private float currentFPS = 60.0f;
    private int frameCount = 0;
    private long lastFPSTime = 0;
    private float fpsEMA = 60.0f; // Exponential moving average

    // Adaptation settings
    private static final float FPS_DECREASE_THRESHOLD = 0.7f; // Drop to 70% of target
    private static final float FPS_INCREASE_THRESHOLD = 0.9f; // Rise to 90% of target
    private static final int DISTANCE_STEP = 2;
    private static final long ADAPTATION_INTERVAL_MS = 2000; // Check every 2 seconds

    private long lastAdaptationTime = 0;
    private boolean enabled = true;

    private AdaptiveRenderDistance() {
        profile = GpuDetector.getProfile();
        currentRenderDistance = getDefaultRenderDistance();
    }

    /**
     * Get singleton instance.
     */
    public static AdaptiveRenderDistance getInstance() {
        if (instance == null) {
            instance = new AdaptiveRenderDistance();
        }
        return instance;
    }

    /**
     * Get default render distance based on GPU tier.
     */
    private int getDefaultRenderDistance() {
        switch (profile.tier) {
            case ULTRA:
                return 32;
            case HIGH:
                return 16;
            case MEDIUM:
                return 12;
            case LOW:
                return 8;
            default:
                return 6;
        }
    }

    /**
     * Update FPS tracking.
     */
    public void onFrame() {
        frameCount++;
        long now = System.currentTimeMillis();

        if (now - lastFPSTime >= 1000) {
            currentFPS = frameCount * 1000.0f / (now - lastFPSTime);
            frameCount = 0;
            lastFPSTime = now;

            // Update exponential moving average
            fpsEMA = 0.7f * fpsEMA + 0.3f * currentFPS;
        }

        // Check for adaptation
        if (enabled && now - lastAdaptationTime >= ADAPTATION_INTERVAL_MS) {
            adaptRenderDistance();
            lastAdaptationTime = now;
        }
    }

    /**
     * Adapt render distance based on current FPS.
     */
    private void adaptRenderDistance() {
        float targetFpsFloat = targetFPS;

        // Too slow - decrease render distance
        if (fpsEMA < targetFpsFloat * FPS_DECREASE_THRESHOLD) {
            int newDistance = Math.max(minRenderDistance, currentRenderDistance - DISTANCE_STEP);
            if (newDistance != currentRenderDistance) {
                currentRenderDistance = newDistance;
                log("[AdaptiveRender] FPS too low (" + (int)fpsEMA + "), reducing to " + currentRenderDistance);
            }
        }
        // Fast enough - try increasing render distance
        else if (fpsEMA > targetFpsFloat * FPS_INCREASE_THRESHOLD) {
            int newDistance = Math.min(maxRenderDistance, currentRenderDistance + DISTANCE_STEP);
            if (newDistance != currentRenderDistance) {
                currentRenderDistance = newDistance;
                log("[AdaptiveRender] FPS good (" + (int)fpsEMA + "), increasing to " + currentRenderDistance);
            }
        }
    }

    /**
     * Get current render distance.
     */
    public int getRenderDistance() {
        return currentRenderDistance;
    }

    /**
     * Set render distance manually.
     */
    public void setRenderDistance(int distance) {
        currentRenderDistance = Math.max(minRenderDistance, Math.min(maxRenderDistance, distance));
    }

    /**
     * Set target FPS.
     */
    public void setTargetFPS(int fps) {
        targetFPS = Math.max(10, Math.min(144, fps));
    }

    /**
     * Get target FPS.
     */
    public int getTargetFPS() {
        return targetFPS;
    }

    /**
     * Get current FPS (EMA).
     */
    public float getCurrentFPS() {
        return fpsEMA;
    }

    /**
     * Enable/disable adaptive rendering.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if adaptive rendering is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get GPU tier name.
     */
    public String getTierName() {
        return profile.getTierName();
    }

    /**
     * Get max texture size from GPU profile.
     */
    public int getMaxTextureSize() {
        return profile.maxTextureSize;
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
