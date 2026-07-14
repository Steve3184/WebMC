package top.steve3184.webmc.world;

import top.steve3184.webmc.game.WebCamera;
import top.steve3184.webmc.teavm.WebLog;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;
import top.steve3184.webmc.teavm.gl.render.RenderEngine;

import java.nio.FloatBuffer;

/**
 * 世界渲染器 - 将区块数据渲染到 WebGL
 */
public class WebWorldRenderer {

    // 方块颜色 (RGB)
    private static final float[] GRASS_COLOR = {0.3f, 0.7f, 0.2f};
    private static final float[] DIRT_COLOR = {0.55f, 0.35f, 0.2f};
    private static final float[] STONE_COLOR = {0.5f, 0.5f, 0.5f};
    private static final float[] WATER_COLOR = {0.2f, 0.4f, 0.8f};

    private WebChunkProvider chunkProvider;
    private WebCamera camera;

    // 渲染缓存
    private FloatBuffer vertexBuffer;
    private int maxVertices = 500000;

    // 统计
    private int trianglesRendered = 0;
    private int chunksRendered = 0;

    public WebWorldRenderer(WebChunkProvider provider, WebCamera camera) {
        this.chunkProvider = provider;
        this.camera = camera;
        this.vertexBuffer = java.nio.ByteBuffer.allocateDirect(maxVertices * 12 * 4)
                .asFloatBuffer();
    }

    /**
     * 渲染世界
     */
    public void render() {
        if (chunkProvider == null || camera == null) {
            return;
        }

        // 更新加载的区块
        chunkProvider.loadChunksAround(camera.getX(), camera.getY(), camera.getZ());

        // 清空统计
        trianglesRendered = 0;
        chunksRendered = 0;

        // 设置相机矩阵
        setupCamera();

        // 渲染每个可见面
        renderVisibleFaces();
    }

    /**
     * 设置相机变换
     */
    private void setupCamera() {
        var gl = WebGLContextHolder.gl();
        if (gl == null) return;

        // 清空缓冲 - 天蓝色背景
        gl.clearColor(0.5f, 0.7f, 1.0f, 1.0f);
        gl.clear(0x00004000); // COLOR_BUFFER_BIT

        // 启用深度测试
        gl.enable(0x0B71); // GL_DEPTH_TEST
        gl.depthFunc(0x0203); // GL_LEQUAL
    }

    /**
     * 渲染可见面
     */
    private void renderVisibleFaces() {
        int playerChunkX = Math.floorDiv((int) camera.getX(), 16);
        int playerChunkZ = Math.floorDiv((int) camera.getZ(), 16);
        int renderDist = 4;

        for (int dx = -renderDist; dx <= renderDist; dx++) {
            for (int dz = -renderDist; dz <= renderDist; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    WebChunkSection chunk = chunkProvider.getChunk(playerChunkX + dx, dy, playerChunkZ + dz);
                    if (chunk != null) {
                        renderChunkFaces(chunk);
                        chunksRendered++;
                    }
                }
            }
        }
    }

    /**
     * 渲染区块的可见面
     */
    private void renderChunkFaces(WebChunkSection section) {
        // 收集面片数据
        float[] vertices = new float[100000];
        int vertexCount = 0;

        int baseX = section.getChunkX() * 16;
        int baseY = section.getChunkY() * 16;
        int baseZ = section.getChunkZ() * 16;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockType block = section.getBlock(x, y, z);
                    if (block == BlockType.AIR) continue;

                    int worldX = baseX + x;
                    int worldY = baseY + y;
                    int worldZ = baseZ + z;

                    float[] color = getBlockColor(block);

                    // 上表面
                    if (isAirOrDifferent(worldX, worldY + 1, worldZ, block)) {
                        addTopFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36; // 6 vertices * 6 floats
                    }

                    // 下表面
                    if (isAirOrDifferent(worldX, worldY - 1, worldZ, block)) {
                        addBottomFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36;
                    }

                    // 北面 (Z-1)
                    if (isAirOrDifferent(worldX, worldY, worldZ - 1, block)) {
                        addNorthFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36;
                    }

                    // 南面 (Z+1)
                    if (isAirOrDifferent(worldX, worldY, worldZ + 1, block)) {
                        addSouthFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36;
                    }

                    // 西面 (X-1)
                    if (isAirOrDifferent(worldX - 1, worldY, worldZ, block)) {
                        addWestFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36;
                    }

                    // 东面 (X+1)
                    if (isAirOrDifferent(worldX + 1, worldY, worldZ, block)) {
                        addEastFace(vertices, vertexCount, worldX, worldY, worldZ, color);
                        vertexCount += 36;
                    }
                }
            }
        }

        if (vertexCount > 0) {
            renderFaces(vertices, vertexCount);
            trianglesRendered += vertexCount / 18; // 6 vertices per face, 3 per triangle
        }
    }

    // 上表面 (Y+)
    private void addTopFace(float[] v, int offset, int x, int y, int z, float[] color) {
        // 三角形 1
        v[offset + 0] = x;     v[offset + 1] = y + 1; v[offset + 2] = z;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x + 1; v[offset + 7] = y + 1; v[offset + 8] = z;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x + 1; v[offset + 13] = y + 1; v[offset + 14] = z + 1;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        // 三角形 2
        v[offset + 18] = x;     v[offset + 19] = y + 1; v[offset + 20] = z;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x + 1; v[offset + 25] = y + 1; v[offset + 26] = z + 1;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x;     v[offset + 31] = y + 1; v[offset + 32] = z + 1;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    // 下表面 (Y-)
    private void addBottomFace(float[] v, int offset, int x, int y, int z, float[] color) {
        v[offset + 0] = x;     v[offset + 1] = y; v[offset + 2] = z + 1;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x + 1; v[offset + 7] = y; v[offset + 8] = z + 1;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x + 1; v[offset + 13] = y; v[offset + 14] = z;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        v[offset + 18] = x;     v[offset + 19] = y; v[offset + 20] = z + 1;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x + 1; v[offset + 25] = y; v[offset + 26] = z;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x;     v[offset + 31] = y; v[offset + 32] = z;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    // 北面 (Z-)
    private void addNorthFace(float[] v, int offset, int x, int y, int z, float[] color) {
        v[offset + 0] = x;     v[offset + 1] = y; v[offset + 2] = z;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x + 1; v[offset + 7] = y; v[offset + 8] = z;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x + 1; v[offset + 13] = y + 1; v[offset + 14] = z;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        v[offset + 18] = x;     v[offset + 19] = y; v[offset + 20] = z;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x + 1; v[offset + 25] = y + 1; v[offset + 26] = z;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x;     v[offset + 31] = y + 1; v[offset + 32] = z;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    // 南面 (Z+)
    private void addSouthFace(float[] v, int offset, int x, int y, int z, float[] color) {
        v[offset + 0] = x + 1; v[offset + 1] = y; v[offset + 2] = z + 1;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x;     v[offset + 7] = y; v[offset + 8] = z + 1;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x;     v[offset + 13] = y + 1; v[offset + 14] = z + 1;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        v[offset + 18] = x + 1; v[offset + 19] = y; v[offset + 20] = z + 1;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x;     v[offset + 25] = y + 1; v[offset + 26] = z + 1;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x + 1; v[offset + 31] = y + 1; v[offset + 32] = z + 1;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    // 西面 (X-)
    private void addWestFace(float[] v, int offset, int x, int y, int z, float[] color) {
        v[offset + 0] = x;     v[offset + 1] = y; v[offset + 2] = z + 1;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x;     v[offset + 7] = y; v[offset + 8] = z;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x;     v[offset + 13] = y + 1; v[offset + 14] = z;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        v[offset + 18] = x;     v[offset + 19] = y; v[offset + 20] = z + 1;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x;     v[offset + 25] = y + 1; v[offset + 26] = z;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x;     v[offset + 31] = y + 1; v[offset + 32] = z + 1;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    // 东面 (X+)
    private void addEastFace(float[] v, int offset, int x, int y, int z, float[] color) {
        v[offset + 0] = x + 1; v[offset + 1] = y; v[offset + 2] = z;
        v[offset + 3] = color[0]; v[offset + 4] = color[1]; v[offset + 5] = color[2];
        v[offset + 6] = x + 1; v[offset + 7] = y; v[offset + 8] = z + 1;
        v[offset + 9] = color[0]; v[offset + 10] = color[1]; v[offset + 11] = color[2];
        v[offset + 12] = x + 1; v[offset + 13] = y + 1; v[offset + 14] = z + 1;
        v[offset + 15] = color[0]; v[offset + 16] = color[1]; v[offset + 17] = color[2];
        v[offset + 18] = x + 1; v[offset + 19] = y; v[offset + 20] = z;
        v[offset + 21] = color[0]; v[offset + 22] = color[1]; v[offset + 23] = color[2];
        v[offset + 24] = x + 1; v[offset + 25] = y + 1; v[offset + 26] = z + 1;
        v[offset + 27] = color[0]; v[offset + 28] = color[1]; v[offset + 29] = color[2];
        v[offset + 30] = x + 1; v[offset + 31] = y + 1; v[offset + 32] = z;
        v[offset + 33] = color[0]; v[offset + 34] = color[1]; v[offset + 35] = color[2];
    }

    /**
     * 检查相邻位置是否为空气或不同方块
     */
    private boolean isAirOrDifferent(int x, int y, int z, BlockType currentBlock) {
        int chunkX = Math.floorDiv(x, 16);
        int chunkY = Math.floorDiv(y, 16);
        int chunkZ = Math.floorDiv(z, 16);

        int localX = ((x % 16) + 16) % 16;
        int localY = ((y % 16) + 16) % 16;
        int localZ = ((z % 16) + 16) % 16;

        WebChunkSection neighbor = chunkProvider.getChunk(chunkX, chunkY, chunkZ);
        if (neighbor == null) return true;

        BlockType neighborBlock = neighbor.getBlock(localX, localY, localZ);
        return neighborBlock != currentBlock;
    }

    /**
     * 获取方块颜色
     */
    private float[] getBlockColor(BlockType type) {
        switch (type) {
            case GRASS: return GRASS_COLOR;
            case DIRT: return DIRT_COLOR;
            case STONE: return STONE_COLOR;
            case WATER: return WATER_COLOR;
            default: return new float[]{1, 1, 1};
        }
    }

    /**
     * 使用 WebGL 渲染面
     */
    private void renderFaces(float[] vertices, int vertexCount) {
        var gl = WebGLContextHolder.gl();
        if (gl == null) return;

        // 简化版本 - 实际需要使用着色器
        // WebLog.info("Rendering " + (vertexCount / 18) + " triangles");
    }

    public int getTrianglesRendered() {
        return trianglesRendered;
    }

    public int getChunksRendered() {
        return chunksRendered;
    }

    public void setCamera(WebCamera camera) {
        this.camera = camera;
    }

    public void setChunkProvider(WebChunkProvider provider) {
        this.chunkProvider = provider;
    }
}
