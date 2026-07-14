package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.JSObject;
import org.teavm.jso.webgl.WebGLRenderingContext;
import top.steve3184.webmc.teavm.WebLog;
import top.steve3184.webmc.teavm.gl.GpuDetector;

/**
 * Adaptive render distance manager.
 * Automatically adjusts render distance based on GPU performance tier.
 */
public final class AdaptiveRenderDistance {

    private RenderEngine renderEngine;
    private GpuDetector.Tier gpuTier;

    public AdaptiveRenderDistance(RenderEngine renderEngine, GpuDetector.Tier gpuTier) {
        this.renderEngine = renderEngine;
        this.gpuTier = gpuTier;
    }

    /**
     * Apply adaptive render distance settings.
     */
    public void apply() {
        int newDistance = calculateOptimalDistance();
        int oldDistance = renderEngine.getRenderDistance();

        if (newDistance != oldDistance) {
            renderEngine.setRenderDistance(newDistance);
            WebLog.info("[AdaptiveRenderDistance] Adjusted render distance from " +
                       oldDistance + " to " + newDistance + " based on " +
                       gpuTier.name + " GPU tier");
        }
    }

    private int calculateOptimalDistance() {
        switch (gpuTier) {
            case ULTRA:
                return 16;
            case HIGH:
                return 12;
            case MEDIUM:
                return 8;
            case LOW:
                return 4;
            default:
                return 6;
        }
    }

    /**
     * Get current render distance.
     */
    public int getCurrentDistance() {
        return renderEngine.getRenderDistance();
    }

    /**
     * Get recommended distance for current GPU tier.
     */
    public int getRecommendedDistance() {
        return calculateOptimalDistance();
    }
}
