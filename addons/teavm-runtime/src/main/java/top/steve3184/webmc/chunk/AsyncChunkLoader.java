package top.steve3184.webmc.chunk;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.slf4j.Logger;
import top.steve3184.webmc.net.WebHttp;
import top.steve3184.webmc.vfs.WebFs;
import top.steve3184.webmc.web.BuildFlags;
import top.steve3184.webmc.render.AdaptiveRenderDistance;

import com.mojang.logging.LogUtils;

/**
 * Async chunk streaming pipeline for web runtime.
 *
 * <p>This class provides async loading of region files from the network
 * instead of relying solely on the in-memory VFS. It supports:</p>
 * <ul>
 *   <li>Async HTTP fetching of region files</li>
 *   <li>Proactive prefetching of adjacent regions</li>
 *   <li>Parallel region loading</li>
 *   <li>Integration with ChunkPrefetcher for movement-based priority</li>
 *   <li>Integration with ChunkMemoryManager for memory optimization</li>
 *   <li>Integration with AdaptiveRenderDistance for dynamic quality</li>
 *   <li>Velocity and direction tracking for predictive prefetching</li>
 *   <li>70/30 directional bias (forward/backward) for chunk loading</li>
 *   <li>2-chunk look-ahead in movement direction</li>
 * </ul>
 */
public final class AsyncChunkLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Base URL for fetching region files from the server */
    private final String regionBaseUrl;

    /** Executor for background operations */
    private final Executor executor;

    /** Reference to the existing IOWorker for writes */
    private final IOWorker ioWorker;

    /** Cache of prefetched region data keyed by region position (x,z) */
    private final java.util.Map<long[], byte[]> regionCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** Maximum number of regions to keep in cache */
    private static final int MAX_REGION_CACHE = 64;

    /** Region file suffix */
    private static final String REGION_EXTENSION = ".mca";

    /** Chunk prefetcher for movement-based loading */
    private final ChunkPrefetcher prefetcher = new ChunkPrefetcher();

    /** Memory manager */
    private final ChunkMemoryManager memoryManager = ChunkMemoryManager.getInstance();

    /** Current render distance */
    private int currentRenderDistance = 8;

    // ============================================================
    // Player movement tracking
    // ============================================================

    /** Previous player position for velocity calculation */
    private double prevPlayerX;
    private double prevPlayerZ;
    private long prevUpdateTime;

    /** Raw velocity before smoothing (blocks per second) */
    private double rawVelocityX;
    private double rawVelocityZ;

    /** Smoothed velocity for movement prediction */
    private double smoothedVelocityX;
    private double smoothedVelocityZ;
    private static final double VELOCITY_SMOOTHING = 0.3;

    /** Movement direction in radians (-PI to PI) */
    private double movementDirection;

    /** Direction change threshold in radians */
    private static final double DIRECTION_CHANGE_THRESHOLD = 0.3;

    /** Minimum velocity to consider player "moving" (blocks per second) */
    private static final double MOVEMENT_THRESHOLD = 0.1;

    /** Predicted player position (look-ahead) */
    private double predictedX;
    private double predictedZ;

    /** Time horizon for prediction in seconds */
    private static final double PREDICTION_HORIZON = 2.0;

    /** Forward/backward bias ratio (70% forward, 30% backward) */
    private static final double FORWARD_BIAS_RATIO = 0.7;
    private static final double BACKWARD_BIAS_RATIO = 0.3;

    public AsyncChunkLoader(String regionBaseUrl, Executor executor, IOWorker ioWorker) {
        this.regionBaseUrl = regionBaseUrl;
        this.executor = executor;
        this.ioWorker = ioWorker;
    }

    // ============================================================
    // Movement tracking methods
    // ============================================================

    /**
     * Update player position and calculate velocity and direction.
     * This enables predictive prefetching based on movement.
     */
    public void updatePlayerPosition(double x, double z) {
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - prevUpdateTime) / 1000.0;

        // Calculate raw velocity
        if (prevUpdateTime > 0 && deltaTime > 0) {
            rawVelocityX = (x - prevPlayerX) / deltaTime;
            rawVelocityZ = (z - prevPlayerZ) / deltaTime;

            // Apply exponential smoothing to velocity
            double alpha = Math.min(1.0, VELOCITY_SMOOTHING * deltaTime);
            smoothedVelocityX = smoothedVelocityX * (1 - alpha) + rawVelocityX * alpha;
            smoothedVelocityZ = smoothedVelocityZ * (1 - alpha) + rawVelocityZ * alpha;

            // Update movement direction if moving
            if (getVelocityMagnitude() > MOVEMENT_THRESHOLD) {
                updateMovementDirection();
            }

            // Update predicted position
            updatePredictedPosition(deltaTime);
        }

        prevPlayerX = x;
        prevPlayerZ = z;
        prevUpdateTime = currentTime;

        // Delegate to prefetcher
        prefetcher.updatePlayerPosition(x, z);

        // Update memory manager
        memoryManager.updatePlayerPosition(x, z);

        // Check if render distance changed
        int newDistance = AdaptiveRenderDistance.getInstance().getRenderDistance();
        if (newDistance != currentRenderDistance) {
            currentRenderDistance = newDistance;
            memoryManager.setRenderDistance(newDistance);
            prefetcher.setRenderDistance(newDistance);
        }
    }

    /**
     * Update movement direction based on smoothed velocity.
     */
    private void updateMovementDirection() {
        double newDirection = Math.atan2(smoothedVelocityZ, smoothedVelocityX);

        // Check for significant direction change
        if (movementDirection == 0) {
            movementDirection = newDirection;
        } else {
            double delta = normalizeAngle(newDirection - movementDirection);
            if (Math.abs(delta) > DIRECTION_CHANGE_THRESHOLD) {
                movementDirection = newDirection;
                LOGGER.debug("Movement direction changed to {}",
                    Math.toDegrees(movementDirection));
            }
        }
    }

    /**
     * Update predicted player position based on current velocity.
     * Uses look-ahead of PREDICTION_HORIZON seconds.
     */
    private void updatePredictedPosition(double deltaTime) {
        // Predict position 2 seconds ahead
        predictedX = prevPlayerX + smoothedVelocityX * PREDICTION_HORIZON;
        predictedZ = prevPlayerZ + smoothedVelocityZ * PREDICTION_HORIZON;
    }

    /**
     * Normalize angle to [-PI, PI].
     */
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * Get the magnitude of the player's velocity.
     * @return Velocity in blocks per second
     */
    public double getVelocityMagnitude() {
        return Math.sqrt(smoothedVelocityX * smoothedVelocityX +
                        smoothedVelocityZ * smoothedVelocityZ);
    }

    /**
     * Get the player's movement direction.
     * @return Direction in radians (-PI to PI), 0 = East, PI/2 = South
     */
    public double getMovementDirection() {
        return movementDirection;
    }

    /**
     * Check if the player is currently moving.
     * @return true if velocity magnitude exceeds threshold
     */
    public boolean isMoving() {
        return getVelocityMagnitude() > MOVEMENT_THRESHOLD;
    }

    /**
     * Get the predicted player position for prefetching.
     * @return Predicted position as array [x, z]
     */
    public double[] getPredictedPosition() {
        return new double[] { predictedX, predictedZ };
    }

    /**
     * Get the normalized velocity direction vector.
     * @return Direction vector [dx, dz] normalized to unit length
     */
    public double[] getVelocityDirection() {
        double mag = getVelocityMagnitude();
        if (mag < MOVEMENT_THRESHOLD) {
            return new double[] { 0, 0 };
        }
        return new double[] { smoothedVelocityX / mag, smoothedVelocityZ / mag };
    }

    /**
     * Get the predicted next chunk position based on movement.
     * @return Predicted ChunkPos or null if not moving
     */
    public ChunkPos getPredictedNextChunk() {
        if (!isMoving()) {
            return null;
        }
        return new ChunkPos(
            ChunkPos.getX(predictedX),
            ChunkPos.getZ(predictedZ)
        );
    }

    /**
     * Get chunks in the forward movement direction (prioritized).
     * @param center Current chunk center
     * @param radius Radius to consider
     * @return List of chunks sorted by priority (forward first)
     */
    public List<ChunkPos> getDirectionalChunks(ChunkPos center, int radius) {
        return prefetcher.getDirectionalChunks(
            center.x, center.z, radius,
            FORWARD_BIAS_RATIO, BACKWARD_BIAS_RATIO
        );
    }

    /**
     * Get chunks in the exact movement direction with look-ahead.
     * Prioritizes chunks 2 chunks ahead in the direction of movement.
     * @param center Current chunk center
     * @param lookAhead Number of chunks to look ahead
     * @return List of chunks sorted by priority
     */
    public List<ChunkPos> getLookAheadChunks(ChunkPos center, int lookAhead) {
        List<ChunkPos> chunks = new java.util.ArrayList<>();

        if (!isMoving()) {
            // Not moving, return standard radial chunks
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    chunks.add(new ChunkPos(center.x + dx, center.z + dz));
                }
            }
            return chunks;
        }

        double[] velDir = getVelocityDirection();
        int dirX = (int) Math.signum(velDir[0]);
        int dirZ = (int) Math.signum(velDir[1]);

        // Forward chunks (in movement direction) - 70%
        int forwardCount = (int) (Math.pow(lookAhead + 1, 2) * 4 * FORWARD_BIAS_RATIO);
        int backwardCount = (int) (Math.pow(lookAhead + 1, 2) * 4 * BACKWARD_BIAS_RATIO);

        // Generate forward chunks with look-ahead
        for (int d = 1; d <= lookAhead + 1; d++) {
            for (int dx = -d; dx <= d; dx++) {
                for (int dz = -d; dz <= d; dz++) {
                    boolean isForward = (dx * dirX >= 0) && (dz * dirZ >= 0);
                    if (!isForward && chunks.size() >= forwardCount) continue;
                    if (isForward && chunks.size() >= forwardCount + backwardCount) continue;

                    ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                    if (!prefetcher.isRecentlyLoaded(pos)) {
                        chunks.add(pos);
                    }
                }
            }
        }

        return chunks;
    }

    // ============================================================
    // Integration with optimization components
    // ============================================================

    /**
     * Get the chunk prefetcher instance.
     */
    public ChunkPrefetcher getPrefetcher() {
        return prefetcher;
    }

    /**
     * Called each frame to update FPS tracking.
     */
    public void onFrame() {
        AdaptiveRenderDistance.getInstance().onFrame();
    }

    /**
     * Get current adaptive render distance.
     */
    public int getAdaptiveRenderDistance() {
        return AdaptiveRenderDistance.getInstance().getRenderDistance();
    }

    // ============================================================
    // Core loading methods
    // ============================================================

    /**
     * Asynchronously load a chunk from a region file.
     * First checks VFS, then falls back to HTTP fetch if needed.
     */
    public CompletableFuture<CompoundTag> loadChunkAsync(ChunkPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();
            String regionPath = getRegionFileName(regionX, regionZ);

            // First try VFS (already loaded)
            CompoundTag chunkData = loadChunkFromVfs(regionPath, pos);
            if (chunkData != null) {
                LOGGER.debug("Chunk {} loaded from VFS", pos);
                memoryManager.onChunkLoaded(pos, estimateChunkSize(chunkData));
                return chunkData;
            }

            // Try prefetched region cache
            byte[] regionData = getPrefetchedRegion(regionX, regionZ);
            if (regionData != null) {
                CompoundTag tag = extractChunkFromRegion(regionData, pos);
                if (tag != null) {
                    LOGGER.debug("Chunk {} loaded from prefetched region", pos);
                    memoryManager.onChunkLoaded(pos, estimateChunkSize(tag));
                    return tag;
                }
            }

            // Fall back to HTTP fetch
            LOGGER.debug("Chunk {} not in cache, attempting HTTP fetch", pos);
            CompoundTag result = loadChunkViaHttp(regionPath, pos);
            if (result != null) {
                memoryManager.onChunkLoaded(pos, estimateChunkSize(result));
                prefetcher.onChunkLoaded(pos);
            }
            return result;
        }, executor);
    }

    /**
     * Estimate chunk data size in bytes.
     */
    private long estimateChunkSize(CompoundTag tag) {
        // Rough estimate: 1KB for empty chunks, up to 1MB for full chunks
        return Math.min(1024 * 1024, Math.max(1024, tag.sizeInNbt()));
    }

    /**
     * Prefetch adjacent regions for better loading performance.
     * Uses movement-based prefetch with 2-chunk look-ahead when player is moving.
     */
    public void prefetchAdjacentRegions(ChunkPos center) {
        if (isMoving()) {
            // Use 2-chunk look-ahead prefetch in movement direction
            prefetchWithLookAhead(center, 2);
        } else {
            // Standard radial prefetch when stationary
            prefetchRadial(center);
        }
    }

    /**
     * Prefetch with directional look-ahead.
     * Prioritizes chunks 2 ahead in movement direction with 70/30 bias.
     */
    private void prefetchWithLookAhead(ChunkPos center, int lookAhead) {
        List<ChunkPos> priorityChunks = getLookAheadChunks(center, lookAhead);

        // Prefetch top chunks (prioritized by direction)
        int prefetchCount = Math.min(16, priorityChunks.size());
        for (int i = 0; i < prefetchCount; i++) {
            ChunkPos pos = priorityChunks.get(i);
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();

            if (getPrefetchedRegion(regionX, regionZ) != null) continue;

            final ChunkPos fpos = pos;
            CompletableFuture.runAsync(() -> {
                prefetchRegion(fpos.getRegionX(), fpos.getRegionZ());
            }, executor);
        }

        // Also prefetch predicted position region if significant
        ChunkPos predicted = getPredictedNextChunk();
        if (predicted != null) {
            int predRegionX = predicted.getRegionX();
            int predRegionZ = predicted.getRegionZ();
            if (getPrefetchedRegion(predRegionX, predRegionZ) == null) {
                CompletableFuture.runAsync(() -> {
                    prefetchRegion(predRegionX, predRegionZ);
                }, executor);
            }
        }
    }

    /**
     * Standard radial prefetch (8 adjacent regions).
     */
    private void prefetchRadial(ChunkPos center) {
        int regionX = center.getRegionX();
        int regionZ = center.getRegionZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int adjX = regionX + dx;
                int adjZ = regionZ + dz;

                if (getPrefetchedRegion(adjX, adjZ) != null) continue;

                final int fx = adjX, fz = adjZ;
                CompletableFuture.runAsync(() -> prefetchRegion(fx, fz), executor);
            }
        }
    }

    /**
     * Movement-based prefetch that prioritizes chunks in travel direction.
     * Uses 70/30 directional bias and 2-chunk look-ahead.
     */
    private void prefetchBasedOnMovement(ChunkPos center) {
        // Use the directional chunks with look-ahead
        List<ChunkPos> priorityChunks = getLookAheadChunks(center, 2);

        // Prefetch top 16 chunks with directional priority
        int prefetchCount = Math.min(16, priorityChunks.size());
        for (int i = 0; i < prefetchCount; i++) {
            ChunkPos pos = priorityChunks.get(i);
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();

            if (getPrefetchedRegion(regionX, regionZ) != null) continue;

            final ChunkPos fpos = pos;
            CompletableFuture.runAsync(() -> {
                prefetchRegion(fpos.getRegionX(), fpos.getRegionZ());
            }, executor);
        }

        // Prefetch the predicted next chunk region
        ChunkPos predicted = getPredictedNextChunk();
        if (predicted != null) {
            int predRegionX = predicted.getRegionX();
            int predRegionZ = predicted.getRegionZ();
            if (getPrefetchedRegion(predRegionX, predRegionZ) == null) {
                CompletableFuture.runAsync(() -> {
                    prefetchRegion(predRegionX, predRegionZ);
                }, executor);
            }
        }
    }

    /**
     * Trigger intelligent prefetch based on current player state.
     */
    public void triggerIntelligentPrefetch(ChunkPos playerChunk) {
        // Update player position
        updatePlayerPosition(playerChunk.x * 16.0, playerChunk.z * 16.0);

        // Trigger prefetch
        prefetchAdjacentRegions(playerChunk);
    }

    /**
     * Prefetch a region file from the network.
     */
    public CompletableFuture<byte[]> prefetchRegion(int regionX, int regionZ) {
        String regionPath = getRegionFileName(regionX, regionZ);

        return CompletableFuture.supplyAsync(() -> {
            long[] key = new long[] { regionX, regionZ };

            // Check if already cached
            if (regionCache.containsKey(key)) {
                return regionCache.get(key);
            }

            // Try VFS first
            byte[] data = WebFs.readBytes(regionPath);
            if (data != null && data.length > 8192) {
                cacheRegion(key, data);
                return data;
            }

            // Fetch from network
            String url = regionBaseUrl + "/" + regionPath;
            LOGGER.debug("Prefetching region {} from {}", regionPath, url);

            data = WebHttp.get(url);
            if (data != null && data.length > 0) {
                cacheRegion(key, data);

                // Also write to VFS for future VFS-based loads
                WebFs.writeBytes(regionPath, data);
                return data;
            }

            LOGGER.warn("Failed to prefetch region {}:{}", regionX, regionZ);
            return null;
        }, executor);
    }

    /**
     * Batch prefetch multiple regions for initial world load.
     */
    public CompletableFuture<Void> prefetchRegionsAsync(int centerRegionX, int centerRegionZ, int radius) {
        java.util.List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int adjX = centerRegionX + dx;
                int adjZ = centerRegionZ + dz;
                futures.add(prefetchRegion(adjX, adjZ).thenAccept(data -> {
                    if (data != null) {
                        LOGGER.debug("Prefetched region {}/{}", adjX, adjZ);
                    }
                }));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Check if a region is available locally (VFS or prefetched).
     */
    public boolean isRegionAvailable(int regionX, int regionZ) {
        return getPrefetchedRegion(regionX, regionZ) != null ||
               WebFs.exists(getRegionFileName(regionX, regionZ));
    }

    /**
     * Get cached region data.
     */
    private byte[] getPrefetchedRegion(int regionX, int regionZ) {
        long[] key = new long[] { regionX, regionZ };
        return regionCache.get(key);
    }

    /**
     * Cache region data with LRU eviction.
     */
    private void cacheRegion(long[] key, byte[] data) {
        if (regionCache.size() >= MAX_REGION_CACHE) {
            // Simple eviction: remove first entry
            var iterator = regionCache.keySet().iterator();
            if (iterator.hasNext()) {
                regionCache.remove(iterator.next());
            }
        }
        regionCache.put(key, data);
    }

    /**
     * Load chunk data from VFS region file.
     */
    private CompoundTag loadChunkFromVfs(String regionPath, ChunkPos pos) {
        if (!BuildFlags.WEB_RUNTIME) {
            return null;
        }

        byte[] regionData = WebFs.readBytes(regionPath);
        if (regionData == null || regionData.length <= 8192) {
            return null;
        }

        return extractChunkFromRegion(regionData, pos);
    }

    /**
     * Extract a chunk's NBT data from region file bytes.
     */
    private CompoundTag extractChunkFromRegion(byte[] regionData, ChunkPos pos) {
        try {
            int localX = pos.getRegionLocalX();
            int localZ = pos.getRegionLocalZ();
            int index = localX + localZ * 32;

            // Read offset from header (4 bytes per entry)
            int offset = readInt(regionData, index * 4);
            if (offset == 0) {
                return null; // Chunk not present
            }

            int sectorNumber = (offset >> 8) & 0xFFFFFF;
            int sectorCount = offset & 0xFF;
            int dataOffset = sectorNumber * 4096;

            if (dataOffset + 5 > regionData.length) {
                LOGGER.warn("Chunk {} offset out of bounds in region", pos);
                return null;
            }

            // Read chunk size (4 bytes) and compression type (1 byte)
            int chunkSize = readInt(regionData, dataOffset) - 1;
            byte compressionType = regionData[dataOffset + 4];

            if (chunkSize <= 0 || chunkSize > sectorCount * 4096) {
                LOGGER.warn("Invalid chunk size {} for chunk {}", chunkSize, pos);
                return null;
            }

            // Extract compressed chunk data
            byte[] chunkData = new byte[chunkSize];
            System.arraycopy(regionData, dataOffset + 5, chunkData, 0, chunkSize);

            // Decompress using the compression type
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(chunkData);
            java.io.DataInputStream dis = new java.io.DataInputStream(
                createDecompressingInputStream(bis, compressionType)
            );

            return NbtIo.read(dis);
        } catch (Exception e) {
            LOGGER.warn("Failed to extract chunk {} from region: {}", pos, e.getMessage());
            return null;
        }
    }

    /**
     * Load chunk via HTTP when not available locally.
     * Uses async HTTP for non-blocking operation.
     */
    private CompletableFuture<CompoundTag> loadChunkViaHttpAsync(String regionPath, ChunkPos pos) {
        String url = regionBaseUrl + "/" + regionPath;
        LOGGER.debug("Async fetching chunk {} from {}", pos, url);

        return WebHttp.getAsync(url).thenApply(data -> {
            if (data == null || data.length <= 8192) {
                LOGGER.warn("Failed to fetch region for chunk {}", pos);
                return null;
            }

            // Cache the region
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();
            cacheRegion(new long[] { regionX, regionZ }, data);

            // Write to VFS for future access
            WebFs.writeBytes(regionPath, data);

            return extractChunkFromRegion(data, pos);
        }).exceptionally(ex -> {
            LOGGER.warn("Async fetch failed for chunk {}: {}", pos, ex.getMessage());
            return null;
        });
    }

    /**
     * Load chunk via HTTP when not available locally (sync fallback).
     */
    private CompoundTag loadChunkViaHttp(String regionPath, ChunkPos pos) {
        String url = regionBaseUrl + "/" + regionPath;
        LOGGER.debug("Fetching chunk {} from {}", pos, url);

        byte[] data = WebHttp.get(url);
        if (data == null || data.length <= 8192) {
            LOGGER.warn("Failed to fetch region for chunk {}", pos);
            return null;
        }

        // Cache the region
        int regionX = pos.getRegionX();
        int regionZ = pos.getRegionZ();
        cacheRegion(new long[] { regionX, regionZ }, data);

        // Write to VFS for future access
        WebFs.writeBytes(regionPath, data);

        return extractChunkFromRegion(data, pos);
    }

    /**
     * Create appropriate decompression stream based on compression type.
     */
    private java.io.InputStream createDecompressingInputStream(java.io.InputStream is, byte type) {
        // Type 1 = GZip, Type 2 = Zlib (deflate), Type 3 = uncompressed
        // Minecraft uses type 2 (Zlib) for Anvil format
        switch (type) {
            case 1:
                try {
                    return new java.util.zip.GZIPInputStream(is);
                } catch (Exception e) {
                    LOGGER.warn("GZip decompression failed", e);
                    return is;
                }
            case 2:
                try {
                    return new java.util.zip.InflaterInputStream(is, new java.util.zip.Inflater(true));
                } catch (Exception e) {
                    LOGGER.warn("Zlib decompression failed", e);
                    return is;
                }
            default:
                return is;
        }
    }

    /**
     * Read a 4-byte little-endian integer from byte array.
     */
    private int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }

    /**
     * Generate the region file name for a given region position.
     */
    public static String getRegionFileName(int regionX, int regionZ) {
        return "/region/r." + regionX + "." + regionZ + REGION_EXTENSION;
    }

    /**
     * Get current region cache size for monitoring.
     */
    public int getRegionCacheSize() {
        return regionCache.size();
    }

    /**
     * Clear the region cache.
     */
    public void clearCache() {
        regionCache.clear();
    }

    private AsyncChunkLoader() {
        this.regionBaseUrl = null;
        this.executor = null;
        this.ioWorker = null;
    }
}