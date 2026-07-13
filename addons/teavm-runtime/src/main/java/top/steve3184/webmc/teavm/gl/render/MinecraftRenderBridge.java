package top.steve3184.webmc.teavm.gl.render;

import top.steve3184.webmc.teavm.WebLog;

/**
 * Minecraft rendering bridge - connects Minecraft's RenderGlobal/WorldRenderer
 * to our high-performance WebGL renderer.
 *
 * This class bridges the gap between Minecraft's chunk rendering pipeline
 * and our TeaVM-optimized rendering system.
 */
public final class MinecraftRenderBridge {

    private static MinecraftRenderBridge instance;
    private static RenderEngine renderEngine;
    private static TextureManager textureManager;
    private static BatchBuffer batchBuffer;

    // Render layer tracking
    private int currentLayer = MinecraftRenderConstants.LAYER_SOLID;
    private int lastTextureId = -1;

    // Stats
    private long totalVertices = 0;
    private long totalTriangles = 0;
    private int drawCallsThisFrame = 0;

    private MinecraftRenderBridge() {
        renderEngine = RenderEngine.getInstance();
        textureManager = TextureManager.getInstance();
        batchBuffer = renderEngine.getBatchBuffer();
    }

    public static MinecraftRenderBridge getInstance() {
        if (instance == null) {
            instance = new MinecraftRenderBridge();
        }
        return instance;
    }

    /**
     * Initialize the rendering bridge.
     * Called during WebMC startup.
     */
    public void init() {
        WebLog.info("[MinecraftRenderBridge] Initializing...");
        renderEngine.init();
        batchBuffer.init();
        textureManager.init();
        WebLog.info("[MinecraftRenderBridge] Initialized successfully");
    }

    /**
     * Begin frame - called at start of each render frame.
     * Matches Minecraft's RenderGlobal.setupRender().
     */
    public void beginFrame() {
        renderEngine.beginFrame();
        drawCallsThisFrame = 0;
        totalVertices = 0;
        totalTriangles = 0;
    }

    /**
     * End frame - called at end of each render frame.
     * Flushes remaining buffers and logs stats.
     */
    public void endFrame() {
        batchBuffer.end();
        renderEngine.endFrame();

        if (drawCallsThisFrame > 0) {
            WebLog.debug(String.format(
                "[RenderBridge] Frame complete: %d draw calls, %d vertices, %d triangles",
                drawCallsThisFrame,
                totalVertices,
                totalTriangles
            ));
        }
    }

    /**
     * Begin rendering a specific layer.
     * Matches Minecraft's RenderType ordering.
     *
     * @param layer One of MinecraftRenderConstants.LAYER_* constants
     */
    public void beginLayer(int layer) {
        if (currentLayer != layer) {
            // Layer changed - flush previous
            batchBuffer.end();
        }

        currentLayer = layer;

        switch (layer) {
            case MinecraftRenderConstants.LAYER_SOLID:
            case MinecraftRenderConstants.LAYER_CUTOUT_MIPPED:
            case MinecraftRenderConstants.LAYER_CUTOUT:
                renderEngine.renderTerrainLayer(0);
                batchBuffer.begin(BatchBuffer.BatchMode.TERRAIN);
                break;

            case MinecraftRenderConstants.LAYER_TRANSLUCENT:
                renderEngine.renderTerrainLayer(3);
                batchBuffer.begin(BatchBuffer.BatchMode.TRANSPARENT);
                break;

            case MinecraftRenderConstants.LAYER_PARTICLES:
                renderEngine.renderParticles();
                batchBuffer.begin(BatchBuffer.BatchMode.PARTICLES);
                break;

            default:
                batchBuffer.begin(BatchBuffer.BatchMode.TERRAIN);
                break;
        }

        lastTextureId = -1;
    }

    /**
     * End current layer.
     */
    public void endLayer() {
        batchBuffer.end();
    }

    /**
     * Bind a texture atlas.
     * Called when Minecraft switches texture binds.
     */
    public void bindTexture(int textureId) {
        if (textureId != lastTextureId) {
            lastTextureId = textureId;
            textureManager.bindTexture(textureId);
        }
    }

    /**
     * Add a terrain vertex with Minecraft's vertex format.
     *
     * Minecraft vertex format (14 floats):
     * 0-2: Position (x, y, z)
     * 3-4: UV (u, v)
     * 5-7: Normal (nx, ny, nz)
     * 8-11: Color (r, g, b, a)
     * 12: Lightmap UV packed (float representation)
     * 13: Overlay packed
     *
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param u Texture U coordinate
     * @param v Texture V coordinate
     * @param nx Normal X component
     * @param ny Normal Y component
     * @param nz Normal Z component
     * @param r Red color (0-1)
     * @param g Green color (0-1)
     * @param b Blue color (0-1)
     * @param a Alpha (0-1)
     * @param light Lightmap UV (packed as float)
     * @param overlay Overlay UV (packed as float)
     */
    public void addTerrainVertex(float x, float y, float z,
                                  float u, float v,
                                  float nx, float ny, float nz,
                                  float r, float g, float b, float a,
                                  int light, int overlay) {
        // Convert packed lightmap/overlay to float
        float lightU = (light & 0xFF) / MinecraftRenderConstants.LIGHTMAP_TEXTURE_SIZE;
        float lightV = ((light >> 8) & 0xFF) / MinecraftRenderConstants.LIGHTMAP_TEXTURE_SIZE;

        batchBuffer.addVertex(x, y, z, u, v, nx, ny, nz, r, g, b, a, light, overlay);
        totalVertices++;

        // Auto-flush if batch is full
        if (batchBuffer.getVertexCount() >= 65536) {
            flushBatch();
        }
    }

    /**
     * Add a full Minecraft vertex.
     * Takes all 14 float components directly.
     */
    public void addMinecraftVertex(float[] vertexData) {
        if (vertexData.length < 14) {
            WebLog.warn("[RenderBridge] Invalid vertex data: expected 14 floats");
            return;
        }

        batchBuffer.addVertex(
            vertexData[0], vertexData[1], vertexData[2],   // Position
            vertexData[3], vertexData[4],                   // UV
            vertexData[5], vertexData[6], vertexData[7],   // Normal
            vertexData[8], vertexData[9], vertexData[10], vertexData[11], // Color
            Float.floatToRawIntBits(vertexData[12]),        // Lightmap
            (int) vertexData[13]                            // Overlay
        );

        totalVertices++;
    }

    /**
     * Add an indexed quad (6 indices for 2 triangles).
     * Called after adding 4 vertices.
     */
    public void addQuad(int baseVertexIndex) {
        batchBuffer.addIndexedQuad(baseVertexIndex);
        totalTriangles += 2; // 2 triangles per quad
    }

    /**
     * Flush the current batch immediately.
     */
    public void flushBatch() {
        RenderStats stats = batchBuffer.getStats();
        drawCallsThisFrame++;
        batchBuffer.flush();
    }

    /**
     * Set camera position and rotation.
     * Called every frame to update view matrices.
     */
    public void setCamera(float x, float y, float z, float yaw, float pitch) {
        renderEngine.setCamera(x, y, z, yaw, pitch);
    }

    /**
     * Check if a bounding box is visible using frustum culling.
     */
    public boolean isBoxVisible(float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ) {
        return renderEngine.isBoxVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Set sky color (matches Minecraft's sky color blending).
     */
    public void setSkyColor(float r, float g, float b) {
        float[] skyColor = renderEngine.getSkyColor();
        skyColor[0] = r;
        skyColor[1] = g;
        skyColor[2] = b;
    }

    /**
     * Set fog parameters matching Minecraft's FogRenderer.
     */
    public void setFog(float start, float end, float density, int mode) {
        renderEngine.setFog(start, end, density, mode);
    }

    /**
     * Get current render stats.
     */
    public RenderStats getStats() {
        return batchBuffer.getStats();
    }

    /**
     * Get FPS from render engine.
     */
    public float getFPS() {
        return renderEngine.getCurrentFps();
    }

    /**
     * Get frame time in milliseconds.
     */
    public long getFrameTime() {
        return renderEngine.getLastFrameTime();
    }

    /**
     * Get total draw calls this frame.
     */
    public int getDrawCalls() {
        return drawCallsThisFrame;
    }

    /**
     * Get render distance setting.
     */
    public int getRenderDistance() {
        return renderEngine.getRenderDistance();
    }

    /**
     * Set render distance.
     */
    public void setRenderDistance(int distance) {
        renderEngine.setRenderDistance(distance);
    }

    /**
     * Reset stats counters.
     */
    public void resetStats() {
        totalVertices = 0;
        totalTriangles = 0;
        drawCallsThisFrame = 0;
        batchBuffer.resetStats();
    }
}
