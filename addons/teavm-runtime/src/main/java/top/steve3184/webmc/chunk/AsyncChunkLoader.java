package top.steve3184.webmc.chunk;

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

import com.mojang.logging.LogUtils;

/**
 * Async chunk streaming pipeline for web runtime.
 *
 * This class provides async loading of region files from the network
 * instead of relying solely on the in-memory VFS. It supports:
 * - Async HTTP fetching of region files
 * - Proactive prefetching of adjacent regions
 * - Parallel region loading
 * - Integration with existing IOWorker for writes
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

    public AsyncChunkLoader(String regionBaseUrl, Executor executor, IOWorker ioWorker) {
        this.regionBaseUrl = regionBaseUrl;
        this.executor = executor;
        this.ioWorker = ioWorker;
    }

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
                return chunkData;
            }

            // Try prefetched region cache
            byte[] regionData = getPrefetchedRegion(regionX, regionZ);
            if (regionData != null) {
                CompoundTag tag = extractChunkFromRegion(regionData, pos);
                if (tag != null) {
                    LOGGER.debug("Chunk {} loaded from prefetched region", pos);
                    return tag;
                }
            }

            // Fall back to HTTP fetch
            LOGGER.debug("Chunk {} not in cache, attempting HTTP fetch", pos);
            return loadChunkViaHttp(regionPath, pos);
        }, executor);
    }

    /**
     * Prefetch adjacent regions for better loading performance.
     * Should be called when a chunk is loaded to preload neighbors.
     */
    public void prefetchAdjacentRegions(ChunkPos center) {
        int regionX = center.getRegionX();
        int regionZ = center.getRegionZ();

        // Prefetch the 8 adjacent regions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int adjX = regionX + dx;
                int adjZ = regionZ + dz;

                // Skip if already cached
                if (getPrefetchedRegion(adjX, adjZ) != null) continue;

                // Async prefetch
                final int fx = adjX, fz = adjZ;
                CompletableFuture.runAsync(() -> prefetchRegion(fx, fz), executor);
            }
        }
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