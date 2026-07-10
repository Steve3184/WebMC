package top.steve3184.webmc.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.Window;

/**
 * Adaptive render distance controller that adjusts chunk rendering distance
 * based on actual frame rate performance using browser's requestAnimationFrame timing.
 *
 * <p>When FPS drops below threshold, render distance is reduced to improve
 * performance. When FPS is stable and high, render distance can be increased
 * for better visuals.</p>
 *
 * <p>Typical settings:</p>
 * <ul>
 *   <li>Min render distance: 4 chunks</li>
 *   <li>Max render distance: 12 chunks</li>
 *   <li>FPS threshold low: 30</li>
 *   <li>FPS threshold high: 55</li>
 *   <li>Low FPS trigger: 3 consecutive seconds</li>
 *   <li>High FPS trigger: 5 consecutive seconds</li>
 * </ul>
 */
public final class AdaptiveRenderDistance {

    /** Minimum render distance in chunks */
    public static final int MIN_RENDER_DISTANCE = 4;

    /** Maximum render distance in chunks */
    public static final int MAX_RENDER_DISTANCE = 12;

    /** Default render distance when FPS is stable */
    public static final int DEFAULT_RENDER_DISTANCE = 8;

    /** FPS below this triggers reduction */
    public static final int FPS_LOW_THRESHOLD = 30;

    /** FPS above this allows increase */
    public static final int FPS_HIGH_THRESHOLD = 55;

    /** Seconds of low FPS before reduction (reduce by 2) */
    public static final int LOW_FPS_TRIGGER_SECONDS = 3;

    /** Seconds of high FPS before increase (increase by 1) */
    public static final int HIGH_FPS_TRIGGER_SECONDS = 5;

    /** Minimum interval between adjustments in milliseconds */
    private static final long ADJUSTMENT_COOLDOWN_MS = 2000;

    /** Current render distance setting */
    private int currentRenderDistance = DEFAULT_RENDER_DISTANCE;

    /** Whether adaptive is enabled */
    private boolean enabled = true;

    /** FPS tracking state */
    private int lowFpsSeconds = 0;
    private int highFpsSeconds = 0;
    private long lastAdjustmentTime = 0;

    /** Current average FPS for display */
    private double currentFps = 60.0;

    /** Whether the monitor loop is running */
    private boolean monitorRunning = false;

    /** RAF callback handle for cleanup */
    private JSObject rafCallback;

    /** Singleton instance */
    private static AdaptiveRenderDistance INSTANCE;

    private AdaptiveRenderDistance() {
        // Private constructor for singleton
    }

    public static AdaptiveRenderDistance getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdaptiveRenderDistance();
        }
        return INSTANCE;
    }

    /**
     * Initialize the adaptive render distance system.
     * Must be called when the game is ready.
     */
    public void initialize() {
        // Sync initial value from Options if available
        syncFromOptions();

        // Start the FPS monitoring loop
        startMonitor();

        log("AdaptiveRenderDistance initialized with renderDistance=" + currentRenderDistance);
    }

    /**
     * Sync current render distance from Options.
     */
    public void syncFromOptions() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                int optionsDist = mc.options.renderDistance().get();
                // Clamp to valid range
                if (optionsDist >= MIN_RENDER_DISTANCE && optionsDist <= MAX_RENDER_DISTANCE) {
                    this.currentRenderDistance = optionsDist;
                    updateJsState();
                    log("AdaptiveRenderDistance synced from Options: " + optionsDist);
                }
            }
        } catch (Exception e) {
            log("Failed to sync from Options: " + e);
        }
    }

    /**
     * Start the FPS monitoring loop using requestAnimationFrame.
     */
    private void startMonitor() {
        if (monitorRunning) {
            return;
        }

        monitorRunning = true;

        // Frame timing state
        final long[] lastFrameTime = { getTimeMillis() };
        final int[] frameCount = { 0 };
        final int[] secondAccumulator = { 0 };

        // Create RAF callback
        EventListener<JSObject> onFrame = (timestamp) -> {
            if (!enabled || !monitorRunning) {
                return;
            }

            long now = getTimeMillis();
            long delta = now - lastFrameTime[0];
            lastFrameTime[0] = now;

            if (delta > 0 && delta < 1000) {
                frameCount[0]++;
                secondAccumulator[0] += (int) delta;

                // Evaluate every second
                if (secondAccumulator[0] >= 1000) {
                    currentFps = frameCount[0];
                    evaluateAndAdjust(currentFps);
                    frameCount[0] = 0;
                    secondAccumulator[0] = 0;
                }
            }

            // Schedule next frame
            requestAnimationFrame(onFrame);
        };

        // Start the loop
        rafCallback = requestAnimationFrame(onFrame);
        log("FPS monitor started");
    }

    /**
     * Stop the FPS monitoring loop.
     */
    public void stopMonitor() {
        monitorRunning = false;
        rafCallback = null;
    }

    /**
     * Evaluate FPS and adjust render distance if needed.
     */
    private synchronized void evaluateAndAdjust(double fps) {
        long now = getTimeMillis();

        // Cooldown check
        if (now - lastAdjustmentTime < ADJUSTMENT_COOLDOWN_MS) {
            return;
        }

        boolean adjusted = false;
        String action = "";

        // Low FPS detection
        if (fps < FPS_LOW_THRESHOLD) {
            lowFpsSeconds++;
            highFpsSeconds = 0;

            if (lowFpsSeconds >= LOW_FPS_TRIGGER_SECONDS) {
                if (currentRenderDistance > MIN_RENDER_DISTANCE) {
                    currentRenderDistance = Math.max(MIN_RENDER_DISTANCE, currentRenderDistance - 2);
                    lowFpsSeconds = 0;
                    lastAdjustmentTime = now;
                    action = "REDUCE";
                    adjusted = true;
                    applyToOptions();
                } else {
                    // At minimum, just reset counter
                    lowFpsSeconds = 0;
                }
            }
        }
        // High FPS detection
        else if (fps > FPS_HIGH_THRESHOLD) {
            highFpsSeconds++;
            lowFpsSeconds = 0;

            if (highFpsSeconds >= HIGH_FPS_TRIGGER_SECONDS) {
                if (currentRenderDistance < MAX_RENDER_DISTANCE) {
                    currentRenderDistance = Math.min(MAX_RENDER_DISTANCE, currentRenderDistance + 1);
                    highFpsSeconds = 0;
                    lastAdjustmentTime = now;
                    action = "INCREASE";
                    adjusted = true;
                    applyToOptions();
                } else {
                    // At maximum, just reset counter
                    highFpsSeconds = 0;
                }
            }
        } else {
            // FPS in normal range - decay counters slowly
            lowFpsSeconds = Math.max(0, lowFpsSeconds - 1);
            highFpsSeconds = Math.max(0, highFpsSeconds - 1);
        }

        if (adjusted) {
            log("AdaptiveRenderDistance: " + action + " to " + currentRenderDistance + " (FPS: " + String.format("%.1f", fps) + ")");
            updateJsState();
        }
    }

    /**
     * Apply current render distance to Minecraft Options.
     */
    private void applyToOptions() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                mc.options.renderDistance().set(currentRenderDistance);
                log("Applied renderDistance=" + currentRenderDistance + " to Options");
            }
        } catch (Exception e) {
            log("Failed to apply to Options: " + e);
        }
    }

    /**
     * Update JavaScript state for UI and game integration.
     */
    private void updateJsState() {
        setJsAdaptiveState(
            currentRenderDistance,
            currentFps,
            enabled,
            lowFpsSeconds,
            highFpsSeconds
        );
    }

    /**
     * Get current render distance.
     */
    public int getRenderDistance() {
        return currentRenderDistance;
    }

    /**
     * Set render distance manually (disables adaptive for this value).
     */
    public void setRenderDistance(int distance) {
        this.currentRenderDistance = Math.max(MIN_RENDER_DISTANCE, Math.min(MAX_RENDER_DISTANCE, distance));
        this.lastAdjustmentTime = getTimeMillis();
        this.lowFpsSeconds = 0;
        this.highFpsSeconds = 0;
        applyToOptions();
        updateJsState();
        log("Manual renderDistance set to " + currentRenderDistance);
    }

    /**
     * Enable or disable adaptive rendering.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            lastAdjustmentTime = 0; // Allow immediate adjustment
            lowFpsSeconds = 0;
            highFpsSeconds = 0;
        } else {
            stopMonitor();
        }
        updateJsState();
        log("AdaptiveRenderDistance " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if adaptive rendering is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get current FPS.
     */
    public double getCurrentFps() {
        return currentFps;
    }

    /**
     * Reset to default settings.
     */
    public void reset() {
        currentRenderDistance = DEFAULT_RENDER_DISTANCE;
        lowFpsSeconds = 0;
        highFpsSeconds = 0;
        lastAdjustmentTime = 0;
        applyToOptions();
        updateJsState();
        log("AdaptiveRenderDistance reset to default");
    }

    /**
     * Get statistics string for debugging.
     */
    public String getStats() {
        return "AdaptiveRD[dist=" + currentRenderDistance
            + ", fps=" + String.format("%.1f", currentFps)
            + ", low=" + lowFpsSeconds + "s"
            + ", high=" + highFpsSeconds + "s"
            + ", enabled=" + enabled + "]";
    }

    // JavaScript integration methods

    @JSBody(params = {}, script = "return Date.now();")
    private static native long getTimeMillis();

    @JSBody(params = {"callback"}, script = "return requestAnimationFrame(callback);")
    private static native JSObject requestAnimationFrame(EventListener<JSObject> callback);

    @JSBody(params = {"dist", "fps", "enabled", "lowSeconds", "highSeconds"}, script =
        "try {" +
        "  if (!window.__webmcState) window.__webmcState = {};" +
        "  window.__webmcState.adaptiveRenderDistance = dist;" +
        "  window.__webmcState.adaptiveFps = fps;" +
        "  window.__webmcState.adaptiveEnabled = enabled;" +
        "  window.__webmcState.adaptiveLowSeconds = lowSeconds;" +
        "  window.__webmcState.adaptiveHighSeconds = highSeconds;" +
        "  if (typeof window.__webmcOnAdaptiveDistanceChange === 'function') {" +
        "    window.__webmcOnAdaptiveDistanceChange(dist, fps, enabled);" +
        "  }" +
        "} catch(e) { console.error('[mc-web/adaptive-rd] JS state update failed:', e); }")
    private static native void setJsAdaptiveState(int dist, double fps, boolean enabled, int lowSeconds, int highSeconds);

    @JSBody(params = "msg", script =
        "try { console.log('[mc-web/adaptive-rd] ' + msg); } catch(e) {}")
    private static native void log(String msg);
}
