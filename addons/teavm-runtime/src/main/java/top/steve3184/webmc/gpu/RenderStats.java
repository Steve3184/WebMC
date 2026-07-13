package top.steve3184.webmc.gpu;

/**
 * Real-time render statistics collector.
 * Tracks FPS, frame times, draw calls, and GPU utilization.
 * Exposes data via JavaScript for browser-based monitoring.
 */
public final class RenderStats {

    // Rolling window for stats
    private static final int FRAME_HISTORY = 60;
    private static final int FPS_HISTORY = 120;

    // Frame timing
    private long frameStartTime = 0;
    private long lastFrameTime = 0;
    private long frameTimeSum = 0;
    private int frameCount = 0;
    private int frameTimeCount = 0;

    // FPS tracking
    private int currentFps = 0;
    private int fpsUpdateInterval = 500; // ms
    private long lastFpsUpdate = 0;
    private int framesSinceFpsUpdate = 0;

    // Frame time history (rolling window)
    private final long[] frameTimeHistory = new long[FRAME_HISTORY];
    private int frameTimeIndex = 0;

    // FPS history
    private final int[] fpsHistory = new int[FPS_HISTORY];
    private int fpsHistoryIndex = 0;

    // Draw call tracking
    private int drawCallsThisFrame = 0;
    private int trianglesThisFrame = 0;
    private int verticesThisFrame = 0;
    private int textureBindsThisFrame = 0;
    private int programSwitchesThisFrame = 0;

    // Cumulative stats
    private long totalDrawCalls = 0;
    private long totalTriangles = 0;
    private long totalVertices = 0;
    private long totalFrames = 0;
    private long sessionStartTime = 0;

    // GPU profile
    private GpuProfile gpuProfile;

    // Whether stats are enabled
    private boolean enabled = false;

    private RenderStats() {}

    private static final RenderStats INSTANCE = new RenderStats();

    public static RenderStats getInstance() {
        return INSTANCE;
    }

    /**
     * Enable statistics collection.
     */
    public void enable() {
        enabled = true;
        sessionStartTime = System.currentTimeMillis();
        gpuProfile = GpuDetector.getProfile();
        reset();
    }

    /**
     * Disable statistics collection.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Called at the start of each frame.
     */
    public void onFrameStart() {
        if (!enabled) return;
        frameStartTime = System.currentTimeMillis();
        drawCallsThisFrame = 0;
        trianglesThisFrame = 0;
        verticesThisFrame = 0;
        textureBindsThisFrame = 0;
        programSwitchesThisFrame = 0;
    }

    /**
     * Called at the end of each frame.
     */
    public void onFrameEnd() {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        long frameTime = now - frameStartTime;
        lastFrameTime = frameTime;

        // Update frame time history
        frameTimeHistory[frameTimeIndex] = frameTime;
        frameTimeIndex = (frameTimeIndex + 1) % FRAME_HISTORY;
        frameTimeSum += frameTime;
        frameTimeCount++;
        totalFrames++;

        // Update FPS
        framesSinceFpsUpdate++;
        if (now - lastFpsUpdate >= fpsUpdateInterval) {
            currentFps = (int) ((framesSinceFpsUpdate * 1000.0) / (now - lastFpsUpdate));
            fpsHistory[fpsHistoryIndex] = currentFps;
            fpsHistoryIndex = (fpsHistoryIndex + 1) % FPS_HISTORY;
            framesSinceFpsUpdate = 0;
            lastFpsUpdate = now;
        }
    }

    /**
     * Record a draw call.
     */
    public void onDrawCall(int vertexCount, int indexCount) {
        if (!enabled) return;
        drawCallsThisFrame++;
        int triCount = indexCount > 0 ? indexCount / 3 : vertexCount / 3;
        trianglesThisFrame += triCount;
        verticesThisFrame += vertexCount;
        totalDrawCalls++;
        totalTriangles += triCount;
        totalVertices += vertexCount;
    }

    /**
     * Record a texture bind.
     */
    public void onTextureBind() {
        if (!enabled) return;
        textureBindsThisFrame++;
    }

    /**
     * Record a program switch.
     */
    public void onProgramSwitch() {
        if (!enabled) return;
        programSwitchesThisFrame++;
    }

    // =============== Getters ===============

    public int getCurrentFps() { return currentFps; }
    public long getLastFrameTime() { return lastFrameTime; }

    public int getDrawCallsThisFrame() { return drawCallsThisFrame; }
    public int getTrianglesThisFrame() { return trianglesThisFrame; }
    public int getVerticesThisFrame() { return verticesThisFrame; }
    public int getTextureBindsThisFrame() { return textureBindsThisFrame; }
    public int getProgramSwitchesThisFrame() { return programSwitchesThisFrame; }

    public long getTotalDrawCalls() { return totalDrawCalls; }
    public long getTotalTriangles() { return totalTriangles; }
    public long getTotalFrames() { return totalFrames; }

    public float getAverageFps() {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < FPS_HISTORY; i++) {
            if (fpsHistory[i] > 0) {
                sum += fpsHistory[i];
                count++;
            }
        }
        return count > 0 ? (float) sum / count : 0f;
    }

    public float getAverageFrameTime() {
        return frameTimeCount > 0 ? (float) frameTimeSum / frameTimeCount : 0f;
    }

    public long getMinFrameTime() {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < FRAME_HISTORY; i++) {
            if (frameTimeHistory[i] > 0) {
                min = Math.min(min, frameTimeHistory[i]);
            }
        }
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxFrameTime() {
        long max = 0;
        for (int i = 0; i < FRAME_HISTORY; i++) {
            max = Math.max(max, frameTimeHistory[i]);
        }
        return max;
    }

    public float getFpsVariance() {
        float avg = getAverageFps();
        float variance = 0;
        int count = 0;
        for (int i = 0; i < FPS_HISTORY; i++) {
            if (fpsHistory[i] > 0) {
                float diff = fpsHistory[i] - avg;
                variance += diff * diff;
                count++;
            }
        }
        return count > 1 ? variance / (count - 1) : 0;
    }

    public float getFpsStdDev() {
        return (float) Math.sqrt(getFpsVariance());
    }

    public int[] getFpsHistory() {
        int[] result = new int[FPS_HISTORY];
        for (int i = 0; i < FPS_HISTORY; i++) {
            result[i] = fpsHistory[i];
        }
        return result;
    }

    public long[] getFrameTimeHistory() {
        long[] result = new long[FRAME_HISTORY];
        for (int i = 0; i < FRAME_HISTORY; i++) {
            result[i] = frameTimeHistory[i];
        }
        return result;
    }

    public GpuProfile getGpuProfile() {
        if (gpuProfile == null) {
            gpuProfile = GpuDetector.getProfile();
        }
        return gpuProfile;
    }

    public long getSessionDuration() {
        return sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0;
    }

    public boolean isEnabled() { return enabled; }

    /**
     * Reset all statistics.
     */
    public void reset() {
        frameTimeSum = 0;
        frameTimeCount = 0;
        frameTimeIndex = 0;
        fpsHistoryIndex = 0;
        currentFps = 0;
        framesSinceFpsUpdate = 0;
        lastFpsUpdate = 0;
        for (int i = 0; i < FRAME_HISTORY; i++) {
            frameTimeHistory[i] = 0;
        }
        for (int i = 0; i < FPS_HISTORY; i++) {
            fpsHistory[i] = 0;
        }
    }

    /**
     * Get a summary string for debugging.
     */
    public String getSummary() {
        return String.format(
            "FPS: %d (avg %.1f) | Frame: %.1fms | DrawCalls: %d | Triangles: %d",
            currentFps, getAverageFps(), getAverageFrameTime(),
            drawCallsThisFrame, trianglesThisFrame
        );
    }

    /**
     * Get JSON representation for JavaScript consumption.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"fps\":").append(currentFps).append(",");
        sb.append("\"avgFps\":").append(getAverageFps()).append(",");
        sb.append("\"frameTime\":").append(getAverageFrameTime()).append(",");
        sb.append("\"minFrameTime\":").append(getMinFrameTime()).append(",");
        sb.append("\"maxFrameTime\":").append(getMaxFrameTime()).append(",");
        sb.append("\"drawCalls\":").append(drawCallsThisFrame).append(",");
        sb.append("\"triangles\":").append(trianglesThisFrame).append(",");
        sb.append("\"vertices\":").append(verticesThisFrame).append(",");
        sb.append("\"textureBinds\":").append(textureBindsThisFrame).append(",");
        sb.append("\"programSwitches\":").append(programSwitchesThisFrame).append(",");
        sb.append("\"gpuTier\":\"").append(gpuProfile != null ? gpuProfile.getTier().name() : "unknown").append("\",");
        sb.append("\"sessionDuration\":").append(getSessionDuration());
        sb.append("}");
        return sb.toString();
    }
}
