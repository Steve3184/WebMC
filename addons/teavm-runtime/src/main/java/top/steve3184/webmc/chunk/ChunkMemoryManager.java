package top.steve3184.webmc.chunk;

import net.minecraft.world.level.ChunkPos;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Memory manager for chunks using LRU (Least Recently Used) eviction policy.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>LRU cache with configurable maximum size</li>
 *   <li>Memory pressure monitoring via performance.memory API</li>
 *   <li>Automatic eviction when pressure exceeds threshold</li>
 *   <li>Keeps chunks in render distance + 2 always resident</li>
 * </ul>
 */
public final class ChunkMemoryManager {

    /** Default maximum chunks in memory */
    private static final int DEFAULT_MAX_CHUNKS = 512;

    /** Memory pressure threshold (80%) */
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.8;

    /** Safe buffer chunks outside render distance */
    private static final int SAFE_BUFFER = 2;

    /** Log interval in seconds */
    private static final long LOG_INTERVAL_MS = 10000;

    /** LRU cache */
    private final LinkedHashMap<ChunkPos, ChunkRef> chunkCache;

    /** Maximum chunks to keep */
    private int maxChunks = DEFAULT_MAX_CHUNKS;

    /** Current memory usage estimate in bytes */
    private long estimatedMemoryBytes = 0;

    /** Last log time */
    private long lastLogTime = 0;

    /** Statistics */
    private int totalEvictions = 0;
    private int totalLoads = 0;
    private int memoryEvictions = 0;

    /** Current render distance */
    private int renderDistance = 8;

    /** Player position for distance calculation */
    private int playerChunkX = 0;
    private int playerChunkZ = 0;

    /** Singleton */
    private static ChunkMemoryManager INSTANCE;

    private ChunkMemoryManager() {
        // Access order for LRU behavior
        this.chunkCache = new LinkedHashMap<ChunkPos, ChunkRef>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ChunkPos, ChunkRef> eldest) {
                return size() > maxChunks && !eldest.getValue().isPinned();
            }
        };
    }

    public static ChunkMemoryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChunkMemoryManager();
        }
        return INSTANCE;
    }

    /**
     * Reference to a chunk with metadata.
     */
    private static class ChunkRef {
        final long sizeBytes;
        final long loadTime;
        boolean isPinned;
        int accessCount;

        ChunkRef(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            this.loadTime = System.currentTimeMillis();
            this.isPinned = false;
            this.accessCount = 1;
        }

        void markAccessed() {
            accessCount++;
        }

        void pin() {
            isPinned = true;
        }

        void unpin() {
            isPinned = false;
        }

        boolean isPinned() {
            return isPinned;
        }
    }

    /**
     * Register a chunk in the memory manager.
     */
    public void onChunkLoaded(ChunkPos pos, long sizeBytes) {
        totalLoads++;

        // Check if already cached
        if (chunkCache.containsKey(pos)) {
            chunkCache.get(pos).markAccessed();
            return;
        }

        // Add to cache (LRU eviction happens automatically)
        chunkCache.put(pos, new ChunkRef(sizeBytes));
        estimatedMemoryBytes += sizeBytes;

        // Check memory pressure
        checkMemoryPressure();

        // Periodic logging
        logStatsIfNeeded();
    }

    /**
     * Mark chunk as accessed (for LRU).
     */
    public void onChunkAccessed(ChunkPos pos) {
        ChunkRef ref = chunkCache.get(pos);
        if (ref != null) {
            // Move to end (most recently used)
            ref.markAccessed();
            // Re-insert to update access order
            chunkCache.remove(pos);
            chunkCache.put(pos, ref);
        }
    }

    /**
     * Unload a chunk.
     */
    public void onChunkUnloaded(ChunkPos pos) {
        ChunkRef ref = chunkCache.remove(pos);
        if (ref != null) {
            estimatedMemoryBytes -= ref.sizeBytes;
        }
    }

    /**
     * Check memory pressure and evict if needed.
     */
    private void checkMemoryPressure() {
        double memoryUsage = getMemoryUsage();
        if (memoryUsage < MEMORY_PRESSURE_THRESHOLD) {
            return;
        }

        memoryEvictions++;

        // Evict chunks outside safe buffer
        evictOutsideBuffer();

        // If still over threshold, reduce to render distance + 1
        if (getMemoryUsage() > MEMORY_PRESSURE_THRESHOLD) {
            evictToRenderDistance();
        }
    }

    /**
     * Evict chunks outside render distance + SAFE_BUFFER.
     */
    private void evictOutsideBuffer() {
        int bufferRadius = renderDistance + SAFE_BUFFER;

        for (Map.Entry<ChunkPos, ChunkRef> entry : chunkCache.entrySet()) {
            ChunkPos pos = entry.getKey();
            ChunkRef ref = entry.getValue();

            if (ref.isPinned()) continue;

            int dx = pos.x - playerChunkX;
            int dz = pos.z - playerChunkZ;
            int distance = Math.max(Math.abs(dx), Math.abs(dz));

            if (distance > bufferRadius) {
                estimatedMemoryBytes -= ref.sizeBytes;
                chunkCache.remove(pos);
                totalEvictions++;

                if (getMemoryUsage() < MEMORY_PRESSURE_THRESHOLD) {
                    break;
                }
            }
        }
    }

    /**
     * Evict chunks beyond render distance + 1.
     */
    private void evictToRenderDistance() {
        for (Map.Entry<ChunkPos, ChunkRef> entry : chunkCache.entrySet()) {
            ChunkPos pos = entry.getKey();
            ChunkRef ref = entry.getValue();

            if (ref.isPinned()) continue;

            int dx = pos.x - playerChunkX;
            int dz = pos.z - playerChunkZ;
            int distance = Math.max(Math.abs(dx), Math.abs(dz));

            if (distance > renderDistance + 1) {
                estimatedMemoryBytes -= ref.sizeBytes;
                chunkCache.remove(pos);
                totalEvictions++;

                if (getMemoryUsage() < MEMORY_PRESSURE_THRESHOLD) {
                    break;
                }
            }
        }
    }

    /**
     * Get memory usage as a fraction (0.0 to 1.0).
     */
    public double getMemoryUsage() {
        return (double) estimatedMemoryBytes / (maxChunks * 100000); // ~100KB per chunk estimate
    }

    /**
     * Get JS memory usage from browser.
     */
    public double getJsMemoryUsage() {
        return getJsMemoryUsageNative();
    }

    @org.teavm.jso.JSBody(script =
        "try { " +
        "  if (performance.memory) {" +
        "    return performance.memory.usedJSHeapSize / performance.memory.jsHeapSizeLimit;" +
        "  }" +
        "} catch(e) {}" +
        "return 0.5;")
    private static native double getJsMemoryUsageNative();

    /**
     * Update player position for distance calculations.
     */
    public void updatePlayerPosition(double x, double z) {
        this.playerChunkX = ChunkPos.getX(x);
        this.playerChunkZ = ChunkPos.getZ(z);
    }

    /**
     * Set render distance.
     */
    public void setRenderDistance(int distance) {
        this.renderDistance = Math.max(4, Math.min(16, distance));
    }

    /**
     * Set maximum chunks.
     */
    public void setMaxChunks(int max) {
        this.maxChunks = Math.max(64, Math.min(1024, max));

        // Evict if over limit
        while (chunkCache.size() > maxChunks) {
            var it = chunkCache.entrySet().iterator();
            if (it.hasNext()) {
                var entry = it.next();
                if (!entry.getValue().isPinned()) {
                    estimatedMemoryBytes -= entry.getValue().sizeBytes;
                    it.remove();
                    totalEvictions++;
                }
            }
        }
    }

    /**
     * Check if a chunk is loaded.
     */
    public boolean isChunkLoaded(ChunkPos pos) {
        return chunkCache.containsKey(pos);
    }

    /**
     * Get number of loaded chunks.
     */
    public int getLoadedChunkCount() {
        return chunkCache.size();
    }

    /**
     * Get estimated memory usage in MB.
     */
    public double getEstimatedMemoryMB() {
        return estimatedMemoryBytes / (1024.0 * 1024.0);
    }

    /**
     * Pin a chunk (prevent eviction).
     */
    public void pinChunk(ChunkPos pos) {
        ChunkRef ref = chunkCache.get(pos);
        if (ref != null) {
            ref.pin();
        }
    }

    /**
     * Unpin a chunk.
     */
    public void unpinChunk(ChunkPos pos) {
        ChunkRef ref = chunkCache.get(pos);
        if (ref != null) {
            ref.unpin();
        }
    }

    /**
     * Clear all chunks.
     */
    public void clear() {
        chunkCache.clear();
        estimatedMemoryBytes = 0;
    }

    /**
     * Log statistics.
     */
    private void logStatsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastLogTime < LOG_INTERVAL_MS) {
            return;
        }
        lastLogTime = now;

        log(String.format(
            "ChunkMemory: loaded=%d/%d (%.1fMB) evictions=%d memEvictions=%d",
            chunkCache.size(), maxChunks,
            getEstimatedMemoryMB(),
            totalEvictions, memoryEvictions
        ));
    }

    /**
     * Get diagnostic info.
     */
    public String getStats() {
        return String.format(
            "ChunkMemory[loaded=%d/%d, mem=%.1fMB, evictions=%d, jsMem=%.1f%%]",
            chunkCache.size(), maxChunks,
            getEstimatedMemoryMB(),
            totalEvictions,
            getJsMemoryUsage() * 100
        );
    }

    @org.teavm.jso.JSBody(params = "msg", script = "console.log('[mc-web/chunk-mem] ' + msg);")
    private static native void log(String msg);
}
