package top.steve3184.webmc.gpu;

import org.teavm.jso.webgl.WebGL2RenderingContext;

/**
 * Batched vertex buffer for efficient rendering of many small draws.
 * Accumulates vertices and indices, then draws them in one call.
 *
 * Benefits:
 * - Reduces draw calls (one batch = one draw call)
 * - Better GPU utilization
 * - Reduced state change overhead
 * - Better cache locality
 */
public final class BatchBuffer {

    private final WebGL2RenderingContext gl;
    private final int capacity;
    private final int[] vertexData;
    private final int[] indexData;
    private int vertexCount = 0;
    private int indexCount = 0;
    private int bufferId = 0;
    private int indexBufferId = 0;
    private boolean initialized = false;

    // Stats
    private int batchesDrawn = 0;
    private int verticesSubmitted = 0;
    private int indicesSubmitted = 0;

    public BatchBuffer(WebGL2RenderingContext gl, int maxVertices, int maxIndices) {
        this.gl = gl;
        this.capacity = maxVertices;
        this.vertexData = new int[maxVertices * VERTEX_STRIDE];
        this.indexData = new int[maxIndices];
    }

    // Vertex layout: position(3) + normal(3) + color(4) + uv(2) = 12 ints
    private static final int VERTEX_STRIDE = 12;

    /**
     * Add a vertex to the batch.
     */
    public void addVertex(float x, float y, float z,
                          float nx, float ny, float nz,
                          float r, float g, float b, float a,
                          float u, float v) {
        if (vertexCount >= capacity) {
            flush();
        }

        int base = vertexCount * VERTEX_STRIDE;
        vertexData[base + 0] = Float.floatToIntBits(x);
        vertexData[base + 1] = Float.floatToIntBits(y);
        vertexData[base + 2] = Float.floatToIntBits(z);
        vertexData[base + 3] = Float.floatToIntBits(nx);
        vertexData[base + 4] = Float.floatToIntBits(ny);
        vertexData[base + 5] = Float.floatToIntBits(nz);
        vertexData[base + 6] = Float.floatToIntBits(r);
        vertexData[base + 7] = Float.floatToIntBits(g);
        vertexData[base + 8] = Float.floatToIntBits(b);
        vertexData[base + 9] = Float.floatToIntBits(a);
        vertexData[base + 10] = Float.floatToIntBits(u);
        vertexData[base + 11] = Float.floatToIntBits(v);
        vertexCount++;
    }

    /**
     * Add an index.
     */
    public void addIndex(int index) {
        if (indexCount >= indexData.length) {
            flush();
        }
        indexData[indexCount++] = index;
    }

    /**
     * Add a quad (two triangles from 4 vertices).
     * Automatically advances the vertex offset.
     */
    public void addQuad(int vertexOffset, float[] positions, float[] normals, float[] colors, float[] uvs) {
        // Triangle 1: 0, 1, 2
        addIndex(vertexOffset + 0);
        addIndex(vertexOffset + 1);
        addIndex(vertexOffset + 2);
        // Triangle 2: 0, 2, 3
        addIndex(vertexOffset + 0);
        addIndex(vertexOffset + 2);
        addIndex(vertexOffset + 3);
    }

    /**
     * Flush the batch to GPU and draw.
     */
    public void flush() {
        if (vertexCount == 0 || indexCount == 0) {
            return;
        }

        ensureInitialized();

        // Upload vertex data
        gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, bufferId);
        float[] vertexFloatData = new float[vertexCount * VERTEX_STRIDE];
        for (int i = 0; i < vertexFloatData.length; i++) {
            vertexFloatData[i] = Float.intBitsToFloat(vertexData[i]);
        }
        gl.bufferData(WebGL2RenderingContext.ARRAY_BUFFER, vertexFloatData, WebGL2RenderingContext.DYNAMIC_DRAW);

        // Upload index data
        gl.bindBuffer(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, indexBufferId);
        int[] indexIntData = new int[indexCount];
        System.arraycopy(indexData, 0, indexIntData, 0, indexCount);
        gl.bufferData(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, indexIntData, WebGL2RenderingContext.DYNAMIC_DRAW);

        // Draw
        gl.drawElements(WebGL2RenderingContext.TRIANGLES, indexCount,
                       WebGL2RenderingContext.UNSIGNED_INT, 0);

        // Update stats
        batchesDrawn++;
        verticesSubmitted += vertexCount;
        indicesSubmitted += indexCount;

        // Reset
        vertexCount = 0;
        indexCount = 0;
    }

    /**
     * Force flush even if buffer is empty.
     */
    public void forceFlush() {
        flush();
    }

    /**
     * Check if batch has data.
     */
    public boolean hasData() {
        return vertexCount > 0 && indexCount > 0;
    }

    /**
     * Get current vertex count.
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Get current index count.
     */
    public int getIndexCount() {
        return indexCount;
    }

    /**
     * Get remaining vertex capacity.
     */
    public int getRemainingCapacity() {
        return capacity - vertexCount;
    }

    /**
     * Get statistics.
     */
    public BatchStats getStats() {
        return new BatchStats(batchesDrawn, verticesSubmitted, indicesSubmitted);
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        batchesDrawn = 0;
        verticesSubmitted = 0;
        indicesSubmitted = 0;
    }

    private void ensureInitialized() {
        if (!initialized) {
            bufferId = gl.createBuffer();
            indexBufferId = gl.createBuffer();
            initialized = true;
        }
    }

    /**
     * Clean up GPU resources.
     */
    public void dispose() {
        if (initialized) {
            if (bufferId != 0) {
                gl.deleteBuffer(bufferId);
                bufferId = 0;
            }
            if (indexBufferId != 0) {
                gl.deleteBuffer(indexBufferId);
                indexBufferId = 0;
            }
            initialized = false;
        }
    }

    /**
     * Batch statistics.
     */
    public static final class BatchStats {
        public final int batchesDrawn;
        public final int verticesSubmitted;
        public final int indicesSubmitted;

        public BatchStats(int batches, int vertices, int indices) {
            this.batchesDrawn = batches;
            this.verticesSubmitted = vertices;
            this.indicesSubmitted = indices;
        }

        public int getVerticesPerBatch() {
            return batchesDrawn > 0 ? verticesSubmitted / batchesDrawn : 0;
        }

        public float getBatchEfficiency() {
            return batchesDrawn > 0 ? (float) indicesSubmitted / batchesDrawn : 0f;
        }

        @Override
        public String toString() {
            return String.format("BatchStats{batches=%d, vertices=%d, indices=%d, avg/batch=%d}",
                batchesDrawn, verticesSubmitted, indicesSubmitted, getVerticesPerBatch());
        }
    }

    // ========== Static factory methods for different use cases ==========

    /**
     * Create a batch buffer sized for chunk rendering.
     */
    public static BatchBuffer createChunkBuffer(WebGL2RenderingContext gl) {
        return new BatchBuffer(gl, 64 * 1024, 128 * 1024);
    }

    /**
     * Create a batch buffer sized for GUI rendering.
     */
    public static BatchBuffer createGuiBuffer(WebGL2RenderingContext gl) {
        return new BatchBuffer(gl, 16 * 1024, 24 * 1024);
    }

    /**
     * Create a batch buffer sized for particle rendering.
     */
    public static BatchBuffer createParticleBuffer(WebGL2RenderingContext gl) {
        return new BatchBuffer(gl, 32 * 1024, 48 * 1024);
    }
}
