// vfs-loader.js - VFS download, decompression, and caching
//
// Handles the full lifecycle of loading game.vfs:
// 1. Check IndexedDB cache
// 2. Download .xz archive if cache miss
// 3. Decompress (stub: simulated for now, real WASM decompressor later)
// 4. Cache decompressed data
// 5. Return ArrayBuffer for WebFs.preload()

(function (global) {
    'use strict';

    // VFS version - should match game.vfs metadata
    // TODO: Extract this dynamically from game.js metadata or separate version.json
    const DEFAULT_VFS_VERSION = '1.21.8';
    const VFS_CACHE_KEY = 'game.vfs';

    /**
     * Progress callback signature:
     * @callback ProgressCallback
     * @param {number} percent - Progress percentage (0-100)
     * @param {string} status - Human-readable status message
     */

    /**
     * Simulated XZ decompression (placeholder until WASM decompressor available).
     * In reality, this would decompress .xz format. For testing, we just return
     * the input or fetch uncompressed data.
     *
     * @param {Uint8Array} compressed - Compressed data
     * @param {ProgressCallback} onProgress - Progress callback
     * @returns {Promise<ArrayBuffer>}
     */
    async function decompressXz(compressed, onProgress) {
        console.log('[VfsLoader] [STUB] Simulating XZ decompression...');

        // Simulate decompression time proportional to size
        const sizeKB = compressed.byteLength / 1024;
        const simulatedMs = Math.min(3000, Math.max(500, sizeKB / 10));

        onProgress && onProgress(60, 'Decompressing...');

        await new Promise(resolve => setTimeout(resolve, simulatedMs));

        onProgress && onProgress(80, 'Decompression complete');

        // For now, return input as-is (assuming it's already decompressed for testing)
        // Real implementation would use lzma-wasm or similar
        console.log('[VfsLoader] [STUB] Decompression complete:',
            (compressed.byteLength / 1024 / 1024).toFixed(2), 'MB');

        return compressed.buffer.slice(
            compressed.byteOffset,
            compressed.byteOffset + compressed.byteLength
        );
    }

    /**
     * Download a file with progress tracking.
     *
     * @param {string} url - File URL
     * @param {ProgressCallback} onProgress - Progress callback
     * @returns {Promise<Uint8Array>}
     */
    async function downloadWithProgress(url, onProgress) {
        console.log('[VfsLoader] Downloading:', url);
        onProgress && onProgress(10, 'Connecting...');

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Download failed: HTTP ' + response.status + ' ' + response.statusText);
        }

        const contentLength = parseInt(response.headers.get('content-length') || '0', 10);
        const reader = response.body && response.body.getReader();

        if (!reader) {
            // Fallback for browsers without ReadableStream support
            console.warn('[VfsLoader] ReadableStream not supported, using arrayBuffer()');
            onProgress && onProgress(30, 'Downloading...');
            const buffer = await response.arrayBuffer();
            onProgress && onProgress(50, 'Download complete');
            return new Uint8Array(buffer);
        }

        const chunks = [];
        let receivedLength = 0;

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            chunks.push(value);
            receivedLength += value.length;

            if (contentLength > 0) {
                const percent = 10 + (receivedLength / contentLength) * 40; // 10-50%
                const sizeMB = (receivedLength / 1024 / 1024).toFixed(2);
                const totalMB = (contentLength / 1024 / 1024).toFixed(2);
                onProgress && onProgress(
                    Math.floor(percent),
                    'Downloading ' + sizeMB + ' / ' + totalMB + ' MB'
                );
            }
        }

        // Concatenate chunks
        const result = new Uint8Array(receivedLength);
        let position = 0;
        for (const chunk of chunks) {
            result.set(chunk, position);
            position += chunk.length;
        }

        const sizeMB = (receivedLength / 1024 / 1024).toFixed(2);
        console.log('[VfsLoader] Downloaded:', sizeMB, 'MB');
        onProgress && onProgress(50, 'Download complete (' + sizeMB + ' MB)');

        return result;
    }

    /**
     * Load VFS data with caching.
     *
     * @param {string} url - VFS file URL (e.g., 'game.vfs' or 'game.vfs.xz')
     * @param {Object} options - Options
     * @param {string} options.version - VFS version for cache validation
     * @param {ProgressCallback} options.onProgress - Progress callback
     * @param {boolean} options.forceDownload - Skip cache and force download
     * @returns {Promise<ArrayBuffer>} Decompressed VFS data
     */
    async function loadVfs(url, options) {
        options = options || {};
        const version = options.version || DEFAULT_VFS_VERSION;
        const onProgress = options.onProgress || null;
        const forceDownload = options.forceDownload || false;

        console.log('[VfsLoader] Loading VFS:', { url: url, version: version, forceDownload: forceDownload });

        // Initialize cache
        let cache = null;
        try {
            onProgress && onProgress(0, 'Initializing cache...');
            cache = await global.VfsCache.create();
        } catch (err) {
            console.warn('[VfsLoader] Cache initialization failed:', err);
        }

        // Check cache
        if (cache && !forceDownload) {
            try {
                onProgress && onProgress(5, 'Checking cache...');
                const cached = await cache.get(VFS_CACHE_KEY, version);
                if (cached) {
                    console.log('[VfsLoader] Cache hit, skipping download');
                    onProgress && onProgress(100, 'Loaded from cache');
                    return cached;
                }
            } catch (err) {
                console.warn('[VfsLoader] Cache read failed:', err);
            }
        }

        // Download
        let compressed;
        try {
            compressed = await downloadWithProgress(url, onProgress);
        } catch (err) {
            console.error('[VfsLoader] Download failed:', err);
            throw new Error('VFS download failed: ' + err.message);
        }

        // Decompress
        let decompressed;
        try {
            onProgress && onProgress(55, 'Decompressing...');
            decompressed = await decompressXz(compressed, onProgress);

            const sizeMB = (decompressed.byteLength / 1024 / 1024).toFixed(2);
            console.log('[VfsLoader] Decompressed size:', sizeMB, 'MB');
        } catch (err) {
            console.error('[VfsLoader] Decompression failed:', err);
            throw new Error('VFS decompression failed: ' + err.message);
        }

        // Cache for next time
        if (cache) {
            try {
                onProgress && onProgress(85, 'Caching...');
                await cache.set(VFS_CACHE_KEY, version, decompressed);
                console.log('[VfsLoader] Cached successfully');
            } catch (err) {
                // Cache write failure is non-fatal
                console.warn('[VfsLoader] Cache write failed (will re-download next time):', err);
            }
        }

        onProgress && onProgress(100, 'VFS loaded');
        return decompressed;
    }

    /**
     * Clear VFS cache (for debugging or forcing re-download).
     * @returns {Promise<void>}
     */
    async function clearVfsCache() {
        console.log('[VfsLoader] Clearing VFS cache...');
        try {
            const cache = await global.VfsCache.create();
            await cache.clear();
            console.log('[VfsLoader] Cache cleared');
        } catch (err) {
            console.error('[VfsLoader] Cache clear failed:', err);
            throw err;
        }
    }

    /**
     * Get storage usage information.
     * @returns {Promise<Object>}
     */
    async function getStorageInfo() {
        try {
            const cache = await global.VfsCache.create();
            const info = await cache.getStorageEstimate();
            console.log('[VfsLoader] Storage info:', info);
            return info;
        } catch (err) {
            console.warn('[VfsLoader] Storage info failed:', err);
            return {
                usage: 0,
                quota: 0,
                usageMB: '0 MB',
                quotaMB: 'unknown',
                percent: 0
            };
        }
    }

    /**
     * Preload VFS and call WebFs.preload() when ready.
     * This is the main integration point called from bootstrap.js.
     *
     * @param {string} url - VFS URL
     * @param {Object} options - Options (same as loadVfs)
     * @returns {Promise<void>}
     */
    async function preloadVfsToWebFs(url, options) {
        options = options || {};

        const startTime = Date.now();
        console.log('[VfsLoader] Starting VFS preload...');

        try {
            // Load VFS data
            const data = await loadVfs(url, options);

            const elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
            console.log('[VfsLoader] VFS loaded in', elapsed, 'seconds');

            // Convert ArrayBuffer to Int8Array for WebFs
            const bytes = new Int8Array(data);

            // Call WebFs.preload() via the global window.main context
            // Note: This assumes WebFs.preload() expects raw bytes, not URL
            // If WebFs still expects sync XHR, we need to bridge differently
            console.log('[VfsLoader] Passing', bytes.length, 'bytes to WebFs...');

            // Store data globally for WebFs to pick up
            global.__webmcVfsData = bytes;
            global.__webmcVfsDataReady = true;

            console.log('[VfsLoader] VFS preload complete');
            return bytes;

        } catch (err) {
            console.error('[VfsLoader] VFS preload failed:', err);
            throw err;
        }
    }

    // Export API
    global.VfsLoader = {
        loadVfs: loadVfs,
        clearCache: clearVfsCache,
        getStorageInfo: getStorageInfo,
        preloadToWebFs: preloadVfsToWebFs,
        DEFAULT_VERSION: DEFAULT_VFS_VERSION
    };

    // Expose debug utilities to console
    if (typeof global.webmc === 'undefined') {
        global.webmc = {};
    }
    global.webmc.clearVfsCache = clearVfsCache;
    global.webmc.getVfsStorageInfo = getStorageInfo;

})(typeof window !== 'undefined' ? window : this);
