package top.steve3184.webmc.gpu;

import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLBuffer;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

/**
 * Batched vertex buffer for efficient rendering of many small draws.
 * Accumulates vertices and indices, then draws them in one call.
 *
 * Benefits:
 * - Reduces draw calls (one batch = one draw call)
 * - Better GPU utilization
 * - Reduced state change overhead
 */
public final class BatchBuffer {

    private static WebGL2RenderingContext gl;

    private final int maxVertices;
    private final int maxIndices;
    private final float[] vertexData;
    private final int[] indexData;
    private int vertexCount = 0;
    private int indexCount = 0;

    private WebGLBuffer vertexBuffer = null;
    private WebGLBuffer indexBuffer = null;
    private boolean initialized = false;

    // Stats
    private int batchesDrawn = 0;
    private int verticesSubmitted = 0;
    private int indicesSubmitted = 0;

    // Vertex layout: position(3) + color(4) + uv(2) = 9 floats
    private static final int VERTEX_STRIDE = 9;

    public BatchBuffer(int maxVertices, int maxIndices) {
        this.maxVertices = maxVertices;
        this.maxIndices = maxIndices;
        this.vertexData = new float[maxVertices * VERTEX_STRIDE];
        this.indexData = new int[maxIndices];
    }

    /**
     * Initialize GL resources.
     */
    public void init() {
        if (initialized) return;
        if (gl == null) gl = WebGLContextHolder.gl();
        if (gl == null) return;

        vertexBuffer = gl.createBuffer();
        indexBuffer = gl.createBuffer();
        initialized = true;
    }

    /**
     * Add a vertex to the batch.
     */
    public void addVertex(float x, float y, float z,
                          float r, float g, float b, float a,
                          float u, float v) {
        if (vertexCount >= maxVertices) {
            flush();
        }

        int idx = vertexCount * VERTEX_STRIDE;
        vertexData[idx] = x;
        vertexData[idx + 1] = y;
        vertexData[idx + 2] = z;
        vertexData[idx + 3] = r;
        vertexData[idx + 4] = g;
        vertexData[idx + 5] = b;
        vertexData[idx + 6] = a;
        vertexData[idx + 7] = u;
        vertexData[idx + 8] = v;
        vertexCount++;
    }

    /**
     * Add an index to the batch.
     */
    public void addIndex(int index) {
        if (indexCount >= maxIndices) {
            flush();
        }
        indexData[indexCount++] = index;
    }

    /**
     * Add a quad using two triangles.
     */
    public void addQuad(float x1, float y1, float x2, float y2,
                        float r, float g, float b, float a,
                        float u1, float v1, float u2, float v2) {
        // Triangle 1
        addVertex(x1, y1, 0, r, g, b, a, u1, v1);
        addVertex(x2, y1, 0, r, g, b, a, u2, v1);
        addVertex(x1, y2, 0, r, g, b, a, u1, v2);

        // Triangle 2
        addVertex(x2, y1, 0, r, g, b, a, u2, v1);
        addVertex(x2, y2, 0, r, g, b, a, u2, v2);
        addVertex(x1, y2, 0, r, g, b, a, u1, v2);
    }

    /**
     * Flush the batch to GPU and draw.
     */
    public void flush() {
        if (vertexCount == 0 || !initialized) return;

        if (gl == null) gl = WebGLContextHolder.gl();
        if (gl == null) return;

        // Upload vertex data
        Float32Array vertices = createFloat32Array(vertexData, 0, vertexCount * VERTEX_STRIDE);
        gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, vertexBuffer);
        gl.bufferData(WebGL2RenderingContext.ARRAY_BUFFER, vertices, WebGL2RenderingContext.DYNAMIC_DRAW);

        // Upload index data
        Int32Array indices = createInt32Array(indexData, 0, indexCount);
        gl.bindBuffer(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);
        gl.bufferData(WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER, indices, WebGL2RenderingContext.DYNAMIC_DRAW);

        // Draw
        gl.drawElements(WebGL2RenderingContext.TRIANGLES, indexCount,
                       WebGL2RenderingContext.UNSIGNED_INT, 0);

        // Update stats
        batchesDrawn++;
        verticesSubmitted += vertexCount;
        indicesSubmitted += indexCount;
        vertexCount = 0;
        indexCount = 0;
    }

    /**
     * Get statistics.
     */
    public BatchStats getStats() {
        return new BatchStats(batchesDrawn, verticesSubmitted, indicesSubmitted);
    }

    /**
     * Clean up GL resources.
     */
    public void destroy() {
        if (vertexBuffer != null) gl.deleteBuffer(vertexBuffer);
        if (indexBuffer != null) gl.deleteBuffer(indexBuffer);
        vertexBuffer = null;
        indexBuffer = null;
        initialized = false;
    }

    // Statistics holder
    public static class BatchStats {
        public final int batchesDrawn;
        public final int verticesSubmitted;
        public final int indicesSubmitted;

        public BatchStats(int batches, int vertices, int indices) {
            this.batchesDrawn = batches;
            this.verticesSubmitted = vertices;
            this.indicesSubmitted = indices;
        }
    }

    // Native helpers
    private static native Float32Array createFloat32Array(float[] data, int offset, int length) /*-{
        return new Float32Array(data.buffer, data.byteOffset + offset * 4, length);
    }-*/;

    private static native Int32Array createInt32Array(int[] data, int offset, int length) /*-{
        return new Int32Array(data.buffer, data.byteOffset + offset * 4, length);
    }-*/;
}
