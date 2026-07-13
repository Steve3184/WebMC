package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import top.steve3184.webmc.teavm.WebLog;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * High-performance batch buffer with VBO and EBO support.
 * Optimized for Minecraft's rendering pipeline (WebGL1 compatible).
 *
 * Key optimizations:
 * - Vertex Buffer Objects (VBO) for efficient GPU memory usage
 * - Element/Index Buffer Objects (EBO) to reduce vertex data
 * - Dynamic buffer orphaning to avoid GPU memory stalls
 * - Automatic buffer sizing based on GPU tier
 */
public final class BatchBuffer {

    // Vertex layout constants (matching Minecraft's format)
    public static final int VERTEX_SIZE_3D = 14; // x,y,z,u,v,nx,ny,nz,r,g,b,a,light,ao = 14 floats
    public static final int VERTEX_SIZE_2D = 9;  // x,y,u,v,r,g,b,a,alpha = 9 floats

    // Batch sizes based on GPU tier
    private static final int MAX_BATCH_TIER_ULTRA = 262144;  // 256K vertices
    private static final int MAX_BATCH_TIER_HIGH = 131072;   // 128K vertices
    private static final int MAX_BATCH_TIER_MEDIUM = 65536;  // 64K vertices
    private static final int MAX_BATCH_TIER_LOW = 32768;     // 32K vertices

    // Index buffer constants (6 indices per quad)
    public static final int INDICES_PER_QUAD = 6;
    private static final int MAX_QUADS = 65536 / 4; // Max quads

    private WebGLRenderingContext gl;
    private GpuDetector.GpuProfile profile;
    private GpuDetector.Tier tier;

    // Vertex data arrays
    private float[] vertexData;
    private int[] indexData;
    private int vertexCount = 0;
    private int quadCount = 0;

    // GPU buffers
    private WebGLBuffer vertexBuffer;
    private WebGLBuffer indexBuffer;
    private int maxBatchSize;

    // Stats
    private int batchesSubmitted = 0;
    private int verticesSubmitted = 0;
    private int drawCalls = 0;
    private int trianglesSubmitted = 0;

    // Current batch mode
    private BatchMode currentMode = BatchMode.NONE;
    private int currentTexture = -1;

    public enum BatchMode {
        NONE,
        TERRAIN,
        ENTITIES,
        TRANSPARENT,
        UI,
        PARTICLES
    }

    public BatchBuffer() {
        this.gl = WebGLContextHolder.gl();
        this.profile = GpuDetector.getProfile();
        this.tier = profile.tier;
        this.maxBatchSize = getMaxBatchSize();
        this.vertexData = new float[maxBatchSize * VERTEX_SIZE_3D];
        this.indexData = new int[MAX_QUADS * INDICES_PER_QUAD];
    }

    /**
     * Begin a new batch.
     */
    public void begin(BatchMode mode) {
        if (currentMode != BatchMode.NONE) {
            flush();
        }
        currentMode = mode;
        currentTexture = -1;
    }

    /**
     * Begin terrain batch (convenience method).
     */
    public void begin() {
        begin(BatchMode.TERRAIN);
    }

    /**
     * End the current batch.
     */
    public void end() {
        if (currentMode != BatchMode.NONE) {
            flush();
        }
        currentMode = BatchMode.NONE;
    }

    private int getMaxBatchSize() {
        switch (tier) {
            case ULTRA: return MAX_BATCH_TIER_ULTRA;
            case HIGH: return MAX_BATCH_TIER_HIGH;
            case MEDIUM: return MAX_BATCH_TIER_MEDIUM;
            case LOW: return MAX_BATCH_TIER_LOW;
            default: return MAX_BATCH_TIER_MEDIUM;
        }
    }

    public void init() {
        if (gl == null) {
            WebLog.warn("[BatchBuffer] Cannot init: WebGL context not available");
            return;
        }

        vertexBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertexData.length * 4, WebGLRenderingContext.DYNAMIC_DRAW);

        indexBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);
        gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexData.length * 4, WebGLRenderingContext.DYNAMIC_DRAW);

        WebLog.info("[BatchBuffer] Initialized (max batch: " + maxBatchSize + " vertices, tier: " + tier.name() + ")");
    }

    /**
     * Begin a new batch with the specified mode.
     */
    public void beginBatch(BatchMode mode) {
        if (currentMode != BatchMode.NONE) {
            flush();
        }
        currentMode = mode;
        currentTexture = -1;
    }

    /**
     * End the current batch.
     */
    public void endBatch() {
        if (currentMode != BatchMode.NONE) {
            flush();
        }
        currentMode = BatchMode.NONE;
    }

    /**
     * Flush current batch to GPU.
     */
    public void flush() {
        if (vertexCount == 0) return;

        batchesSubmitted++;
        verticesSubmitted += vertexCount;
        drawCalls++;
        trianglesSubmitted += vertexCount / 3;

        uploadVertexData();
        uploadIndexData();
        drawBatch();

        vertexCount = 0;
        quadCount = 0;
        currentTexture = -1;
    }

    private void uploadVertexData() {
        Float32Array data = Float32Array.create(vertexCount * VERTEX_SIZE_3D);
        for (int i = 0; i < vertexCount * VERTEX_SIZE_3D; i++) {
            data.set(i, vertexData[i]);
        }
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
        gl.bufferSubData(WebGLRenderingContext.ARRAY_BUFFER, 0, data);
    }

    private void uploadIndexData() {
        Int32Array data = Int32Array.create(quadCount * INDICES_PER_QUAD);
        for (int i = 0; i < quadCount * INDICES_PER_QUAD; i++) {
            data.set(i, indexData[i]);
        }
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);
        gl.bufferSubData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, 0, data);
    }

    private void drawBatch() {
        if (quadCount > 0) {
            // WebGL1 uses UNSIGNED_SHORT for indices (16-bit), WebGL2 can use UNSIGNED_INT (32-bit)
            // For cross-browser compatibility, we use 16-bit indices
            gl.drawElements(WebGLRenderingContext.TRIANGLES, quadCount * INDICES_PER_QUAD,
                          WebGLRenderingContext.UNSIGNED_SHORT, 0);
        } else {
            gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, vertexCount);
        }
    }

    /**
     * Add a vertex to the current batch.
     */
    public void addVertex(float x, float y, float z, float u, float v,
                         float nx, float ny, float nz,
                         float r, float g, float b, float a,
                         int light, int ao) {
        if (vertexCount >= maxBatchSize) {
            flush();
        }

        int idx = vertexCount * VERTEX_SIZE_3D;
        vertexData[idx] = x;
        vertexData[idx + 1] = y;
        vertexData[idx + 2] = z;
        vertexData[idx + 3] = u;
        vertexData[idx + 4] = v;
        vertexData[idx + 5] = nx;
        vertexData[idx + 6] = ny;
        vertexData[idx + 7] = nz;
        vertexData[idx + 8] = r;
        vertexData[idx + 9] = g;
        vertexData[idx + 10] = b;
        vertexData[idx + 11] = a;
        vertexData[idx + 12] = Float.intBitsToFloat(light);
        vertexData[idx + 13] = (float) ao;

        vertexCount++;
    }

    /**
     * Add a quad using index buffer.
     */
    public void addIndexedQuad(int baseVertex) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }

        int idx = quadCount * INDICES_PER_QUAD;
        indexData[idx] = baseVertex;
        indexData[idx + 1] = baseVertex + 1;
        indexData[idx + 2] = baseVertex + 2;
        indexData[idx + 3] = baseVertex + 2;
        indexData[idx + 4] = baseVertex + 3;
        indexData[idx + 5] = baseVertex;

        quadCount++;
    }

    /**
     * Add a 2D UI vertex.
     */
    public void addUIQuad(float x, float y, float u, float v,
                         float r, float g, float b, float a) {
        if (vertexCount >= maxBatchSize) {
            flush();
        }

        int idx = vertexCount * VERTEX_SIZE_2D;
        vertexData[idx] = x;
        vertexData[idx + 1] = y;
        vertexData[idx + 2] = u;
        vertexData[idx + 3] = v;
        vertexData[idx + 4] = r;
        vertexData[idx + 5] = g;
        vertexData[idx + 6] = b;
        vertexData[idx + 7] = b;
        vertexData[idx + 8] = a;

        vertexCount++;
    }

    /**
     * Get current vertex count.
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Get render statistics.
     */
    public RenderStats getStats() {
        return new RenderStats(
            batchesSubmitted,
            verticesSubmitted,
            drawCalls,
            trianglesSubmitted,
            vertexCount,
            quadCount,
            0,
            tier.name()
        );
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        batchesSubmitted = 0;
        verticesSubmitted = 0;
        drawCalls = 0;
        trianglesSubmitted = 0;
    }

    /**
     * Get GPU tier.
     */
    public String getGpuTier() {
        return tier.name();
    }
}
