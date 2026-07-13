package top.steve3184.webmc.teavm.gl.render;

import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import top.steve3184.webmc.teavm.gl.GpuDetector;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * High-performance batch buffer for efficient rendering.
 * Uses vertex buffer objects (VBO) and instanced rendering where supported.
 */
public final class BatchBuffer {

    public static final int MAX_BATCH_SIZE = 65536; // Max vertices per batch
    public static final int VERTEX_SIZE_3D = 8; // position(3) + texCoord(2) + normal(3) = 8 floats
    public static final int VERTEX_SIZE_2D = 7; // position(2) + texCoord(2) + color(4) = 7 floats (padded)

    private WebGLRenderingContext gl;
    private GpuDetector.GpuProfile profile;

    // Vertex data buffers
    private float[] vertexData;
    private int vertexCount = 0;

    // GPU buffers
    private WebGLBuffer vertexBuffer;
    private boolean bufferDirty = true;

    // Stats
    private int batchesSubmitted = 0;
    private int verticesSubmitted = 0;

    // Batch mode
    private BatchMode currentMode = BatchMode.NONE;
    private int textureId = 0;

    public enum BatchMode {
        NONE,
        TILES_3D,
        UI_2D,
        TERRAIN
    }

    public BatchBuffer() {
        gl = WebGLContextHolder.gl();
        profile = GpuDetector.getProfile();
        vertexData = new float[MAX_BATCH_SIZE * VERTEX_SIZE_3D];
    }

    /**
     * Initialize the batch buffer with GPU context.
     */
    public void init() {
        if (gl == null) return;

        vertexBuffer = gl.createBuffer();
        log("[BatchBuffer] Initialized for " + profile.getTierName() + " GPU");
    }

    /**
     * Begin a new batch.
     */
    public void begin(BatchMode mode) {
        if (mode != currentMode) {
            flush();
            currentMode = mode;
        }
        clear();
    }

    /**
     * Add a 3D tile vertex.
     */
    public void addTile3D(float x, float y, float z, float u, float v,
                         float nx, float ny, float nz) {
        if (vertexCount >= MAX_BATCH_SIZE) {
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

        vertexCount++;
        bufferDirty = true;
    }

    /**
     * Add a 2D UI vertex.
     */
    public void addQuad2D(float x, float y, float u, float v, float r, float g, float b, float a) {
        if (vertexCount >= MAX_BATCH_SIZE) {
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
        vertexData[idx + 7] = a;

        vertexCount++;
        bufferDirty = true;
    }

    /**
     * Flush current batch to GPU.
     */
    public void flush() {
        if (vertexCount == 0 || gl == null || vertexBuffer == null) {
            return;
        }

        // Upload vertex data
        Float32Array data = createFloat32Array(vertexCount * VERTEX_SIZE_3D);
        for (int i = 0; i < vertexCount * VERTEX_SIZE_3D; i++) {
            data.set(i, vertexData[i]);
        }

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, data, WebGLRenderingContext.DYNAMIC_DRAW);

        // Draw
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, vertexCount);

        batchesSubmitted++;
        verticesSubmitted += vertexCount;

        clear();
        bufferDirty = false;
    }

    private static native Float32Array createFloat32Array(int length) /*-{
        return new Float32Array(length);
    }-*/;

    /**
     * Clear current batch.
     */
    public void clear() {
        vertexCount = 0;
        bufferDirty = false;
    }

    /**
     * End current batch and flush.
     */
    public void end() {
        flush();
        currentMode = BatchMode.NONE;
    }

    /**
     * Get vertex count.
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Get batches submitted count.
     */
    public int getBatchesSubmitted() {
        return batchesSubmitted;
    }

    /**
     * Get vertices submitted count.
     */
    public int getVerticesSubmitted() {
        return verticesSubmitted;
    }

    /**
     * Reset stats.
     */
    public void resetStats() {
        batchesSubmitted = 0;
        verticesSubmitted = 0;
    }

    /**
     * Check if instancing is supported.
     */
    public boolean supportsInstancing() {
        return profile.supportsInstancing;
    }

    /**
     * Get recommended batch size for current GPU.
     */
    public int getRecommendedBatchSize() {
        switch (profile.tier) {
            case ULTRA:
                return MAX_BATCH_SIZE;
            case HIGH:
                return MAX_BATCH_SIZE / 2;
            case MEDIUM:
                return MAX_BATCH_SIZE / 4;
            default:
                return 4096;
        }
    }

    /**
     * Destroy buffers.
     */
    public void destroy() {
        if (gl != null && vertexBuffer != null) {
            gl.deleteBuffer(vertexBuffer);
            vertexBuffer = null;
        }
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
