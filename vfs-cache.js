// vfs-cache.js - IndexedDB wrapper for VFS caching
//
// Stores decompressed VFS data in IndexedDB to avoid re-downloading and
// re-decompressing on every page load. Supports version tracking to
// invalidate cache when game.vfs updates.

(function (global) {
    'use strict';

    const DB_NAME = 'webmc_vfs';
    const DB_VERSION = 1;
    const STORE_NAME = 'vfs_data';

    /**
     * VFS cache entry structure:
     * {
     *   key: string,           // Cache key (e.g., 'game.vfs')
     *   version: string,       // VFS version for invalidation
     *   data: ArrayBuffer,     // Decompressed VFS data
     *   timestamp: number,     // Cache creation time
     *   size: number          // Data size in bytes
     * }
     */

    class VfsCache {
        constructor() {
            this.db = null;
            this.initPromise = null;
        }

        /**
         * Initialize IndexedDB connection. Idempotent.
         * @returns {Promise<void>}
         */
        init() {
            if (this.initPromise) {
                return this.initPromise;
            }

            this.initPromise = new Promise((resolve, reject) => {
                console.log('[VfsCache] Opening IndexedDB:', DB_NAME);
                const request = indexedDB.open(DB_NAME, DB_VERSION);

                request.onerror = () => {
                    const err = request.error || new Error('IndexedDB open failed');
                    console.error('[VfsCache] Open failed:', err);
                    reject(err);
                };

                request.onsuccess = () => {
                    this.db = request.result;
                    console.log('[VfsCache] Opened successfully');
                    resolve();
                };

                request.onupgradeneeded = (event) => {
                    console.log('[VfsCache] Upgrading schema to version', DB_VERSION);
                    const db = event.target.result;

                    if (!db.objectStoreNames.contains(STORE_NAME)) {
                        const store = db.createObjectStore(STORE_NAME, { keyPath: 'key' });
                        store.createIndex('version', 'version', { unique: false });
                        store.createIndex('timestamp', 'timestamp', { unique: false });
                        console.log('[VfsCache] Created object store:', STORE_NAME);
                    }
                };
            });

            return this.initPromise;
        }

        /**
         * Get cached data for the given key and version.
         * @param {string} key - Cache key
         * @param {string} expectedVersion - Expected version string
         * @returns {Promise<ArrayBuffer|null>} Cached data or null if miss/stale
         */
        async get(key, expectedVersion) {
            try {
                await this.init();

                return new Promise((resolve, reject) => {
                    const transaction = this.db.transaction([STORE_NAME], 'readonly');
                    const store = transaction.objectStore(STORE_NAME);
                    const request = store.get(key);

                    request.onerror = () => {
                        console.error('[VfsCache] Read failed:', request.error);
                        reject(request.error);
                    };

                    request.onsuccess = () => {
                        const entry = request.result;

                        if (!entry) {
                            console.log('[VfsCache] Miss:', key);
                            resolve(null);
                            return;
                        }

                        if (entry.version !== expectedVersion) {
                            console.log('[VfsCache] Version mismatch:', {
                                key: key,
                                cached: entry.version,
                                expected: expectedVersion
                            });
                            // Asynchronously delete stale cache
                            this.delete(key).catch(err =>
                                console.warn('[VfsCache] Failed to delete stale entry:', err)
                            );
                            resolve(null);
                            return;
                        }

                        const sizeMB = (entry.size / 1024 / 1024).toFixed(2);
                        console.log('[VfsCache] Hit:', key, `(${sizeMB} MB, v${entry.version})`);
                        resolve(entry.data);
                    };
                });
            } catch (err) {
                console.error('[VfsCache] Get failed:', err);
                return null;
            }
        }

        /**
         * Store data in cache with version.
         * @param {string} key - Cache key
         * @param {string} version - Version string
         * @param {ArrayBuffer} data - Data to cache
         * @returns {Promise<void>}
         */
        async set(key, version, data) {
            try {
                await this.init();

                const entry = {
                    key: key,
                    version: version,
                    data: data,
                    timestamp: Date.now(),
                    size: data.byteLength
                };

                return new Promise((resolve, reject) => {
                    const transaction = this.db.transaction([STORE_NAME], 'readwrite');
                    const store = transaction.objectStore(STORE_NAME);
                    const request = store.put(entry);

                    request.onerror = () => {
                        console.error('[VfsCache] Write failed:', request.error);
                        reject(request.error);
                    };

                    request.onsuccess = () => {
                        const sizeMB = (data.byteLength / 1024 / 1024).toFixed(2);
                        console.log('[VfsCache] Cached:', key, `(${sizeMB} MB, v${version})`);
                        resolve();
                    };
                });
            } catch (err) {
                console.error('[VfsCache] Set failed:', err);
                throw err;
            }
        }

        /**
         * Delete a cache entry.
         * @param {string} key - Cache key
         * @returns {Promise<void>}
         */
        async delete(key) {
            try {
                await this.init();

                return new Promise((resolve, reject) => {
                    const transaction = this.db.transaction([STORE_NAME], 'readwrite');
                    const store = transaction.objectStore(STORE_NAME);
                    const request = store.delete(key);

                    request.onerror = () => reject(request.error);
                    request.onsuccess = () => {
                        console.log('[VfsCache] Deleted:', key);
                        resolve();
                    };
                });
            } catch (err) {
                console.error('[VfsCache] Delete failed:', err);
                throw err;
            }
        }

        /**
         * Clear all cache entries.
         * @returns {Promise<void>}
         */
        async clear() {
            try {
                await this.init();

                return new Promise((resolve, reject) => {
                    const transaction = this.db.transaction([STORE_NAME], 'readwrite');
                    const store = transaction.objectStore(STORE_NAME);
                    const request = store.clear();

                    request.onerror = () => reject(request.error);
                    request.onsuccess = () => {
                        console.log('[VfsCache] Cleared all entries');
                        resolve();
                    };
                });
            } catch (err) {
                console.error('[VfsCache] Clear failed:', err);
                throw err;
            }
        }

        /**
         * Get storage quota information.
         * @returns {Promise<{usage: number, quota: number, usageMB: string, quotaMB: string, percent: number}>}
         */
        async getStorageEstimate() {
            if ('storage' in navigator && 'estimate' in navigator.storage) {
                try {
                    const estimate = await navigator.storage.estimate();
                    const usage = estimate.usage || 0;
                    const quota = estimate.quota || 0;
                    return {
                        usage: usage,
                        quota: quota,
                        usageMB: (usage / 1024 / 1024).toFixed(2) + ' MB',
                        quotaMB: quota > 0 ? (quota / 1024 / 1024).toFixed(2) + ' MB' : 'unlimited',
                        percent: quota > 0 ? Math.floor((usage / quota) * 100) : 0
                    };
                } catch (err) {
                    console.warn('[VfsCache] Storage estimate failed:', err);
                }
            }
            return {
                usage: 0,
                quota: 0,
                usageMB: '0 MB',
                quotaMB: 'unknown',
                percent: 0
            };
        }
    }

    // Fallback cache for when IndexedDB is unavailable
    class VfsCacheFallback {
        constructor() {
            this.memoryCache = new Map();
            console.warn('[VfsCache] Using in-memory fallback (cache lost on refresh)');
        }

        async init() {
            // No-op for memory cache
        }

        async get(key, expectedVersion) {
            const entry = this.memoryCache.get(key);
            if (!entry) {
                console.log('[VfsCache] [Memory] Miss:', key);
                return null;
            }
            if (entry.version !== expectedVersion) {
                console.log('[VfsCache] [Memory] Version mismatch:', key);
                this.memoryCache.delete(key);
                return null;
            }
            console.log('[VfsCache] [Memory] Hit:', key);
            return entry.data;
        }

        async set(key, version, data) {
            this.memoryCache.set(key, { version: version, data: data });
            const sizeMB = (data.byteLength / 1024 / 1024).toFixed(2);
            console.log('[VfsCache] [Memory] Cached:', key, `(${sizeMB} MB)`);
        }

        async delete(key) {
            this.memoryCache.delete(key);
            console.log('[VfsCache] [Memory] Deleted:', key);
        }

        async clear() {
            this.memoryCache.clear();
            console.log('[VfsCache] [Memory] Cleared all entries');
        }

        async getStorageEstimate() {
            let usage = 0;
            for (const entry of this.memoryCache.values()) {
                usage += entry.data.byteLength;
            }
            return {
                usage: usage,
                quota: 0,
                usageMB: (usage / 1024 / 1024).toFixed(2) + ' MB',
                quotaMB: 'memory only',
                percent: 0
            };
        }
    }

    /**
     * Create a VFS cache instance, falling back to memory if IndexedDB unavailable.
     * @returns {Promise<VfsCache|VfsCacheFallback>}
     */
    async function createVfsCache() {
        // Check IndexedDB availability
        if (!('indexedDB' in global)) {
            console.warn('[VfsCache] IndexedDB not available');
            return new VfsCacheFallback();
        }

        try {
            const cache = new VfsCache();
            await cache.init();
            return cache;
        } catch (err) {
            console.error('[VfsCache] IndexedDB init failed, using fallback:', err);
            return new VfsCacheFallback();
        }
    }

    // Export API
    global.VfsCache = {
        create: createVfsCache,
        VfsCache: VfsCache,
        VfsCacheFallback: VfsCacheFallback
    };

})(typeof window !== 'undefined' ? window : this);
