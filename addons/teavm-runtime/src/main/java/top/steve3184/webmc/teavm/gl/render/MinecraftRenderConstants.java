package top.steve3184.webmc.teavm.gl.render;

/**
 * Minecraft-style rendering constants and configuration.
 * Matches Minecraft Java Edition's rendering pipeline.
 *
 * Reference: net.minecraft.client.renderer.RenderChunk (1.21.x)
 */
public final class MinecraftRenderConstants {

    // Block/chunk constants
    public static final int SECTION_SIZE = 16;  // Chunks are 16x16x16 blocks
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256; // Max world height
    public static final int MAX_RENDER_DISTANCE = 32;
    public static final int MIN_RENDER_DISTANCE = 2;

    // Buffer sizes - matching Minecraft's buffer allocations
    public static final int VERTEX_BUFFER_SIZE = 262144;  // 256K vertices per chunk section
    public static final int INDEX_BUFFER_SIZE = 393216;   // Indices for quad mesh

    // Render layer flags (matching Minecraft's RenderType)
    public static final int LAYER_SOLID = 0x01;
    public static final int LAYER_CUTOUT_MIPPED = 0x02;
    public static final int LAYER_CUTOUT = 0x04;
    public static final int LAYER_TRANSLUCENT = 0x08;
    public static final int LAYER_TRIPWIRE = 0x10;
    public static final int LAYER_PARTICLES = 0x20;
    public static final int LAYER_WEATHER = 0x40;
    public static final int LAYER_END_GATEWAY = 0x80;
    public static final int LAYER_LIGHT = 0x100;

    // Vertex format strides (matching Minecraft's VertexFormat)
    public static final int VERTEX_FORMAT_STRIDE = 14;  // 14 floats per vertex
    public static final int VERTEX_FORMAT_BLOCK_STRIDE = 14;  // Block vertex format

    // Vertex element offsets
    public static final int POSITION_OFFSET = 0;        // 3 floats (x, y, z)
    public static final int COLOR_OFFSET = 3;         // 4 floats (r, g, b, a) or packed
    public static final int UV_OFFSET = 7;            // 2 floats (u, v)
    public static final int OVERLAY_OFFSET = 9;       // 2 shorts packed (lightmap, overlay)
    public static final int NORMAL_OFFSET = 11;       // 3 bytes packed (nx, ny, nz) + padding
    public static final int PADDING_OFFSET = 12;      // 2 floats padding for alignment

    // Texture atlas
    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 256;
    public static final int ATLAS_SIZE = 256;

    // Lightmap
    public static final int LIGHTMAP_SIZE = 16;
    public static final float LIGHTMAP_INTENSITY = 1.0f;

    // Fog settings - matching Minecraft's FogRenderer
    public static final float FOG_BASE_DENSITY = 0.001f;
    public static final float FOG_START_MULTIPLIER = 0.75f;
    public static final float FOG_END_MULTIPLIER = 1.0f;
    public static final float FOG_COLOR_MULTIPLIER = 1.0f;

    // Render distance multipliers by GPU tier
    public static final float RENDER_DISTANCE_ULTRA = 1.0f;
    public static final float RENDER_DISTANCE_HIGH = 0.75f;
    public static final float RENDER_DISTANCE_MEDIUM = 0.5f;
    public static final float RENDER_DISTANCE_LOW = 0.25f;

    // Frustum culling
    public static final boolean DEFAULT_FRUSTUM_CULLING = true;
    public static final float FRUSTUM_MARGIN = 1.0f; // Blocks margin for frustum culling

    // Buffer clearing
    public static final int COLOR_BUFFER_CLEAR = 0xFF000000; // Sky color buffer
    public static final float DEPTH_BUFFER_CLEAR = 1.0f;    // Far plane
    public static final int STENCIL_BUFFER_CLEAR = 0;

    // GL state defaults
    public static final float DEFAULT_LINE_WIDTH = 1.0f;
    public static final float DEFAULT_POINT_SIZE = 1.0f;
    public static final int DEFAULT_VIEWPORT_WIDTH = 1280;
    public static final int DEFAULT_VIEWPORT_HEIGHT = 720;

    // Performance thresholds
    public static final long FRAME_TIME_WARNING_MS = 50;  // Warn if frame > 50ms
    public static final long FRAME_TIME_CRITICAL_MS = 100; // Critical if frame > 100ms
    public static final int FPS_STABLE_THRESHOLD = 55;
    public static final int FPS_GOOD_THRESHOLD = 45;

    // Chunk rebuild scheduling
    public static final int CHUNK_REBUILD_BATCH_SIZE = 4;
    public static final long CHUNK_REBUILD_MIN_INTERVAL_MS = 10;

    // Vertex data packing
    public static final float VERTEX_COMPRESSION_SCALE = 4096.0f; // For packing positions
    public static final int VERTEX_PACKING_BITS = 12;

    // Lightmap packed UV (Minecraft uses 240 as max light value)
    public static final float LIGHTMAP_TEXTURE_SIZE = 240.0f;
    public static final int LIGHTMAP_MAX_VALUE = 240;

    // Block face culling - Minecraft uses neighbor checks
    public static final boolean USE_BLOCK_FACE_CULLING = true;

    // State sorting
    public static final int MAX_STATE_SORT_BUCKETS = 32;

    private MinecraftRenderConstants() {
        // Utility class - prevent instantiation
    }
}
