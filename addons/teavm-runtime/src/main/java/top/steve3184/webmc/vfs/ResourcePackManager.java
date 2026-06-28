package top.steve3184.webmc.vfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Resource pack manager for WebMC browser runtime.
 *
 * <p>Handles loading and prioritization of resource packs with the following sources:</p>
 * <ol>
 *   <li><b>Built-in pack</b> - Pre-bundled vanilla assets from MCVF file</li>
 *   <li><b>Local packs</b> - User-selected .zip/.mcpack files from local filesystem</li>
 *   <li><b>Remote packs</b> - Resource packs loaded from HTTP URLs</li>
 * </ol>
 *
 * <p>Resource resolution order (first match wins):</p>
 * <ol>
 *   <li>Local packs (highest priority - user customizations, in user-defined order)</li>
 *   <li>Remote packs (in user-defined order)</li>
 *   <li>Built-in pack (lowest priority - vanilla fallback)</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>
 *   // Initialize after WebFs is ready
 *   ResourcePackManager manager = new ResourcePackManager();
 *   manager.initBuiltinPack();
 *
 *   // Add a local pack from file picker
 *   manager.addLocalPack("My Pack", zipBytes, 9);
 *
 *   // Add a remote pack from URL (async)
 *   manager.addRemotePackAsync("HD Textures", "https://example.com/pack.zip", 9, (pack, error) -> {
 *       if (pack != null) {
 *           // Successfully added
 *       }
 *   });
 *
 *   // Load all enabled packs in priority order
 *   manager.loadAll();
 *
 *   // Read a resource (automatically uses override priority)
 *   byte[] data = manager.readResource("minecraft", "textures/block/dirt.png");
 *
 *   // Reload resources after changes
 *   manager.reload();
 * </pre>
 */
public final class ResourcePackManager {

    /** Current version format supported (Minecraft 1.20.x) */
    public static final int SUPPORTED_PACK_FORMAT = 15;

    /** Built-in pack is always at the bottom of the stack */
    private ResourcePackInfo builtinPack;

    /** List of enabled packs in priority order (local first, then remote, then builtin) */
    private final List<ResourcePackInfo> enabledPacks;

    /** All loaded packs (including disabled ones) for UI management */
    private final Map<String, ResourcePackInfo> allPacks;

    /** Map of resource path (namespace:path) to pack ID (for override tracking) */
    private final Map<String, String> resourceOverrides;

    /** Current resource manifest */
    private volatile ResourceManifest manifest;

    /** Loading state */
    private volatile boolean loading;

    /** Thread pool for async operations */
    private final ExecutorService executor;

    /** Load progress callback */
    private LoadProgressCallback progressCallback;

    /** Resource pack loading listeners */
    private final List<ResourcePackLoadListener> listeners;

    /** Pack format compatibility tolerance (Minecraft versions) */
    private static final int PACK_FORMAT_TOLERANCE = 2;

    public interface LoadProgressCallback {
        void onProgress(float progress, String status);
    }

    public interface ResourcePackLoadListener {
        void onPackLoaded(ResourcePackInfo pack);
        void onPackLoadFailed(ResourcePackInfo pack, String error);
        void onAllPacksLoaded();
    }

    public interface AsyncPackCallback {
        void onComplete(ResourcePackInfo pack, String error);
    }

    public ResourcePackManager() {
        this.enabledPacks = new ArrayList<>();
        this.allPacks = new HashMap<>();
        this.resourceOverrides = new HashMap<>();
        this.loading = false;
        this.listeners = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(2);
    }

    /**
     * Initialize with the built-in vanilla pack.
     * Should be called after WebFs.preload() completes.
     */
    public void initBuiltinPack() {
        this.builtinPack = ResourcePackInfo.builtin(
            "WebMC Vanilla",
            "WebMC browser runtime bundled assets",
            SUPPORTED_PACK_FORMAT
        );
        this.builtinPack.setLoaded(true);
        addPackInternal(builtinPack);
    }

    /**
     * Add a local resource pack from bytes (e.g., from file picker).
     *
     * @param name Display name for the pack
     * @param zipBytes Raw bytes of the .zip/.mcpack file
     * @param packFormat Minecraft pack format version
     * @return The created ResourcePackInfo, or null if parsing failed
     */
    public ResourcePackInfo addLocalPack(String name, byte[] zipBytes, int packFormat) {
        return addLocalPack(name, zipBytes, packFormat, null);
    }

    /**
     * Add a local resource pack from bytes with completion callback.
     *
     * @param name Display name for the pack
     * @param zipBytes Raw bytes of the .zip/.mcpack file
     * @param packFormat Minecraft pack format version
     * @param callback Optional callback when pack is added
     * @return The created ResourcePackInfo, or null if parsing failed
     */
    public ResourcePackInfo addLocalPack(String name, byte[] zipBytes, int packFormat, Consumer<ResourcePackInfo> callback) {
        try {
            // Parse pack.mcmeta to get real name/description
            PackMetadata metadata = parsePackMetadata(zipBytes);

            // Use metadata values if available
            String packName = metadata.name != null ? metadata.name : name;
            String description = metadata.description != null ? metadata.description : "Local resource pack";

            // Check format compatibility
            if (metadata.packFormat > 0) {
                packFormat = metadata.packFormat;
            }
            boolean compatible = isCompatible(packFormat);

            UUID id = UUID.randomUUID();
            ResourcePackInfo pack = new ResourcePackInfo(
                "local_" + id.toString(),
                packName,
                description,
                "local",
                "memory://" + id.toString(),
                packFormat,
                zipBytes.length
            );
            pack.setCompatible(compatible);

            // Store zip data in WebFs for later loading
            String packPath = "/resourcepacks/local_" + id.toString() + ".zip";
            WebFs.writeBytes(packPath, zipBytes);

            addPackInternal(pack);

            if (callback != null) {
                callback.accept(pack);
            }

            return pack;
        } catch (Exception e) {
            WebFs.log("ResourcePackManager: Failed to add local pack: " + e);
            return null;
        }
    }

    /**
     * Check if a pack format is compatible with the current Minecraft version.
     */
    public boolean isCompatible(int packFormat) {
        return Math.abs(packFormat - SUPPORTED_PACK_FORMAT) <= PACK_FORMAT_TOLERANCE;
    }

    /**
     * Add a remote resource pack from URL.
     *
     * @param name Display name for the pack
     * @param url Direct download URL for the .zip/.mcpack file
     * @param packFormat Minecraft pack format version
     * @return The created ResourcePackInfo
     */
    public ResourcePackInfo addRemotePack(String name, String url, int packFormat) {
        return addRemotePack(name, url, packFormat, null);
    }

    /**
     * Add a remote resource pack from URL with completion callback.
     *
     * @param name Display name for the pack
     * @param url Direct download URL for the .zip/.mcpack file
     * @param packFormat Minecraft pack format version
     * @param callback Optional callback when pack is added
     * @return The created ResourcePackInfo
     */
    public ResourcePackInfo addRemotePack(String name, String url, int packFormat, AsyncPackCallback callback) {
        UUID id = UUID.randomUUID();
        ResourcePackInfo pack = new ResourcePackInfo(
            "remote_" + id.toString(),
            name,
            "Remote resource pack: " + name,
            "remote",
            url,
            packFormat,
            0  // Size unknown until downloaded
        );
        pack.setCompatible(isCompatible(packFormat));

        addPackInternal(pack);

        if (callback != null) {
            callback.onComplete(pack, null);
        }

        return pack;
    }

    /**
     * Add a remote resource pack and download it asynchronously.
     * This is the preferred method for remote packs as it doesn't block the UI.
     *
     * @param name Display name for the pack
     * @param url Direct download URL for the .zip/.mcpack file
     * @param packFormat Minecraft pack format version
     * @param callback Called when download completes (pack or error)
     */
    public void addRemotePackAsync(String name, String url, int packFormat, AsyncPackCallback callback) {
        UUID id = UUID.randomUUID();
        ResourcePackInfo pack = new ResourcePackInfo(
            "remote_" + id.toString(),
            name,
            "Remote resource pack: " + name,
            "remote",
            url,
            packFormat,
            0
        );
        pack.setCompatible(isCompatible(packFormat));

        addPackInternal(pack);

        // Download in background
        executor.submit(() -> {
            try {
                reportProgress(-1, "Downloading " + name + "...");

                // Fetch the pack
                byte[] zipData = WebFs.fetchSync(url);

                if (zipData == null || zipData.length == 0) {
                    pack.setErrorMessage("Download failed or empty response");
                    if (callback != null) {
                        callback.onComplete(null, "Download failed or empty response");
                    }
                    return;
                }

                pack.estimatedSize = zipData.length;

                // Parse metadata
                PackMetadata metadata = parsePackMetadata(zipData);
                if (metadata.name != null) {
                    pack.name = metadata.name;
                }
                if (metadata.description != null) {
                    pack.description = metadata.description;
                }
                if (metadata.packFormat > 0) {
                    pack = new ResourcePackInfo(
                        pack.id,
                        pack.name,
                        pack.description,
                        pack.sourceType,
                        pack.sourceLocation,
                        metadata.packFormat,
                        zipData.length
                    );
                    pack.setCompatible(isCompatible(metadata.packFormat));
                    // Update in allPacks map
                    allPacks.put(pack.id, pack);
                    // Update in enabledPacks list
                    for (int i = 0; i < enabledPacks.size(); i++) {
                        if (enabledPacks.get(i).id.equals(pack.id)) {
                            enabledPacks.set(i, pack);
                            break;
                        }
                    }
                }

                // Cache the downloaded data
                String cachePath = "/resourcepacks/" + pack.id + ".zip";
                WebFs.writeBytes(cachePath, zipData);

                reportProgress(1.0f, "Downloaded " + name);

                if (callback != null) {
                    callback.onComplete(pack, null);
                }

            } catch (Exception e) {
                pack.setErrorMessage("Download failed: " + e.getMessage());
                WebFs.log("ResourcePackManager: Failed to download remote pack: " + e);
                if (callback != null) {
                    callback.onComplete(null, e.getMessage());
                }
            }
        });
    }

    /**
     * Remove a pack by ID.
     * This will also clear any resources extracted by this pack from VFS.
     */
    public boolean removePack(String packId) {
        synchronized (allPacks) {
            ResourcePackInfo pack = allPacks.get(packId);
            if (pack == null) return false;

            // Clear pack's resources from VFS
            WebFs.clearPackFromVfs(packId);

            // Remove from enabled list
            enabledPacks.remove(pack);

            // Remove from all packs
            allPacks.remove(packId);

            // Rebuild override map
            rebuildOverrideMap();

            return true;
        }
    }

    /**
     * Clear all resources from a specific pack from VFS without removing the pack.
     * This allows temporarily disabling a pack's contributions while keeping it in the list.
     */
    public void clearPackResources(String packId) {
        WebFs.clearPackFromVfs(packId);

        // Remove pack's resources from override map
        synchronized (allPacks) {
            resourceOverrides.entrySet().removeIf(e -> e.getValue().equals(packId));
        }
    }

    /**
     * Restore resources for a pack (after clearing).
     */
    public boolean restorePackResources(String packId) {
        synchronized (allPacks) {
            ResourcePackInfo pack = allPacks.get(packId);
            if (pack == null || !pack.isEnabled() || !pack.isLoaded()) return false;

            // Re-register pack's resources
            registerPackResources(pack);
            return true;
        }
    }

    /**
     * Set pack enabled/disabled state.
     * When disabled, the pack's resources are cleared from VFS.
     * When enabled, the pack's resources are restored to VFS.
     */
    public void setPackEnabled(String packId, boolean enabled) {
        synchronized (allPacks) {
            ResourcePackInfo pack = allPacks.get(packId);
            if (pack == null) return;

            boolean wasEnabled = pack.isEnabled();
            pack.setEnabled(enabled);

            if (wasEnabled && !enabled) {
                // Disabling: clear pack's resources
                clearPackResources(packId);
            } else if (!wasEnabled && enabled) {
                // Enabling: restore pack's resources if loaded
                if (pack.isLoaded()) {
                    restorePackResources(packId);
                }
            }

            // Rebuild enabled list
            rebuildEnabledList();
        }
    }

    /**
     * Move a pack to a new position in the priority order.
     * Higher in list = higher priority (overrides lower packs).
     */
    public void movePack(String packId, int newPosition) {
        synchronized (allPacks) {
            ResourcePackInfo pack = allPacks.get(packId);
            if (pack == null) return;

            if (!enabledPacks.remove(pack)) return;

            int pos = Math.max(0, Math.min(newPosition, enabledPacks.size()));
            enabledPacks.add(pos, pack);

            rebuildOverrideMap();
        }
    }

    /**
     * Get all registered packs.
     */
    public List<ResourcePackInfo> getAllPacks() {
        synchronized (allPacks) {
            return new ArrayList<>(allPacks.values());
        }
    }

    /**
     * Get enabled packs in priority order.
     */
    public List<ResourcePackInfo> getEnabledPacks() {
        synchronized (allPacks) {
            return new ArrayList<>(enabledPacks);
        }
    }

    /**
     * Get a pack by ID.
     */
    public ResourcePackInfo getPack(String packId) {
        synchronized (allPacks) {
            return allPacks.get(packId);
        }
    }

    /**
     * Set progress callback for load operations.
     */
    public void setProgressCallback(LoadProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * Add a load listener.
     */
    public void addLoadListener(ResourcePackLoadListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Load all enabled resource packs.
     * Packs are loaded in priority order (local > remote > builtin).
     */
    public void loadAll() {
        if (loading) {
            WebFs.log("ResourcePackManager: Already loading, skipping");
            return;
        }

        loading = true;
        reportProgress(0, "Starting resource pack loading...");

        try {
            List<ResourcePackInfo> packs;
            synchronized (allPacks) {
                packs = new ArrayList<>(enabledPacks);
            }

            int total = packs.size();
            int current = 0;

            for (ResourcePackInfo pack : packs) {
                if (!pack.isEnabled()) continue;

                current++;
                float progress = (float) current / total;
                reportProgress(progress, "Loading " + pack.name + "...");

                boolean success = loadPack(pack);

                synchronized (listeners) {
                    for (ResourcePackLoadListener listener : listeners) {
                        if (success) {
                            listener.onPackLoaded(pack);
                        } else {
                            listener.onPackLoadFailed(pack, pack.getErrorMessage());
                        }
                    }
                }
            }

            // Rebuild manifest after all packs loaded
            reportProgress(0.95f, "Building resource manifest...");
            rebuildManifest();

            reportProgress(1.0f, "Resource packs loaded");
            loading = false;

            synchronized (listeners) {
                for (ResourcePackLoadListener listener : listeners) {
                    listener.onAllPacksLoaded();
                }
            }

        } catch (Exception e) {
            WebFs.log("ResourcePackManager: Load failed: " + e);
            loading = false;
            throw new RuntimeException("Failed to load resource packs", e);
        }
    }

    /**
     * Reload all resource packs and rebuild the manifest.
     */
    public void reload() {
        WebFs.log("ResourcePackManager: Reloading all packs");

        // Clear all pack resources from VFS
        WebFs.clearAllPacksFromVfs();

        // Clear override map
        resourceOverrides.clear();

        // Reset all pack loaded states (except builtin)
        synchronized (allPacks) {
            for (ResourcePackInfo pack : allPacks.values()) {
                if (!"builtin".equals(pack.sourceType)) {
                    pack.setLoaded(false);
                }
            }
        }

        loadAll();
    }

    /**
     * Clear all resource packs and reset the system to default (builtin only).
     * This removes all local and remote packs from the manager.
     */
    public void clearAll() {
        WebFs.log("ResourcePackManager: Clearing all packs");

        // Clear all pack resources from VFS
        WebFs.clearAllPacksFromVfs();

        // Clear override map
        resourceOverrides.clear();

        // Remove all non-builtin packs
        synchronized (allPacks) {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, ResourcePackInfo> entry : allPacks.entrySet()) {
                if (!"builtin".equals(entry.getValue().sourceType)) {
                    toRemove.add(entry.getKey());
                }
            }
            for (String packId : toRemove) {
                allPacks.remove(packId);
            }

            // Rebuild enabled list
            rebuildEnabledList();
        }

        // Rebuild manifest
        rebuildManifest();

        WebFs.log("ResourcePackManager: Cleared all packs, " +
                   (builtinPack != null ? "builtin pack retained" : "no builtin pack"));
    }

    /**
     * Shutdown the resource pack manager.
     * Releases resources and shuts down the executor service.
     */
    public void shutdown() {
        WebFs.log("ResourcePackManager: Shutting down");
        clearAll();
        executor.shutdown();
    }

    /**
     * Check if a resource exists in any loaded pack.
     */
    public boolean hasResource(String namespace, String path) {
        return getResourceSource(namespace, path) != null;
    }

    /**
     * Get the pack ID that provides a specific resource.
     */
    public String getResourceSource(String namespace, String path) {
        String key = namespace + ":" + path;
        return resourceOverrides.get(key);
    }

    /**
     * Read a resource, respecting pack priority.
     * Returns bytes from the highest priority pack that contains the resource.
     *
     * <p>Priority order (highest to lowest):</p>
     * <ol>
     *   <li>Local packs (in user-defined order)</li>
     *   <li>Remote packs (in user-defined order)</li>
     *   <li>Built-in pack (vanilla fallback)</li>
     * </ol>
     *
     * @param namespace Resource namespace (e.g., "minecraft")
     * @param path Resource path within namespace (e.g., "textures/block/dirt.png")
     * @return Resource bytes, or null if not found
     */
    public byte[] readResource(String namespace, String path) {
        String key = namespace + ":" + path;
        String packId = resourceOverrides.get(key);

        if (packId == null) {
            // No override found, fall back to VFS (builtin resources)
            String fullPath = "/assets/" + namespace + "/" + path;
            return WebFs.readBytes(fullPath);
        }

        // Find the pack and read from its VFS path
        ResourcePackInfo pack;
        synchronized (allPacks) {
            pack = allPacks.get(packId);
        }

        if (pack == null) {
            // Pack no longer exists, use default VFS
            String fullPath = "/assets/" + namespace + "/" + path;
            return WebFs.readBytes(fullPath);
        }

        // Construct pack-specific path
        String packPath = "/resourcepacks/" + pack.id + ".zip";

        // For loaded packs, resources are extracted to VFS directly
        // So we just read from the standard path
        String fullPath = "/assets/" + namespace + "/" + path;
        return WebFs.readBytes(fullPath);
    }

    /**
     * Read a resource directly from a specific pack.
     * Use this when you need to access a specific pack's version of a resource.
     *
     * @param packId The pack ID to read from
     * @param namespace Resource namespace
     * @param path Resource path within namespace
     * @return Resource bytes, or null if not found in this pack
     */
    public byte[] readResourceFromPack(String packId, String namespace, String path) {
        ResourcePackInfo pack;
        synchronized (allPacks) {
            pack = allPacks.get(packId);
        }

        if (pack == null) return null;

        // Read from pack ZIP file directly
        String packPath = "/resourcepacks/" + pack.id + ".zip";
        byte[] zipData = WebFs.readBytes(packPath);

        if (zipData == null) return null;

        // Extract specific file from ZIP
        String resourcePath = "assets/" + namespace + "/" + path;
        return extractFileFromZip(zipData, resourcePath);
    }

    /**
     * Extract a single file from a ZIP archive.
     */
    private byte[] extractFileFromZip(byte[] zipData, String targetPath) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(targetPath)) {
                    byte[] data = readAllBytes(zis);
                    zis.closeEntry();
                    return data;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            WebFs.log("ResourcePackManager: Failed to extract file from ZIP: " + e);
        }
        return null;
    }

    private boolean loadPack(ResourcePackInfo pack) {
        try {
            switch (pack.sourceType) {
                case "builtin":
                    return loadBuiltinPack(pack);
                case "local":
                    return loadLocalPack(pack);
                case "remote":
                    return loadRemotePack(pack);
                default:
                    pack.setErrorMessage("Unknown source type: " + pack.sourceType);
                    return false;
            }
        } catch (Exception e) {
            pack.setErrorMessage(e.getMessage());
            return false;
        }
    }

    private boolean loadBuiltinPack(ResourcePackInfo pack) {
        // Builtin pack resources are already loaded via WebFs.preload()
        pack.setLoaded(true);
        pack.setErrorMessage(null);

        // Register builtin resources in override map
        if (manifest != null) {
            for (String namespace : manifest.getNamespaces()) {
                List<?> resources = manifest.listResources(namespace);
                for (Object res : resources) {
                    // ResourceLocation is the type used by manifest
                    String key = namespace + ":" + extractPath(res);
                    resourceOverrides.put(key, pack.id);
                }
            }
        }

        return true;
    }

    private boolean loadLocalPack(ResourcePackInfo pack) {
        String packPath = "/resourcepacks/" + pack.id + ".zip";
        byte[] zipData = WebFs.readBytes(packPath);

        if (zipData == null) {
            pack.setErrorMessage("Pack file not found: " + packPath);
            return false;
        }

        return extractPackToVfs(pack, zipData);
    }

    private boolean loadRemotePack(ResourcePackInfo pack) {
        if (!pack.isLoaded() || pack.estimatedSize == 0) {
            // Need to download first
            byte[] zipData = fetchRemotePack(pack);
            if (zipData == null) {
                return false;
            }

            // Cache in VFS
            String cachePath = "/resourcepacks/" + pack.id + ".zip";
            WebFs.writeBytes(cachePath, zipData);
            pack.setLoaded(true);
        }

        // Now load from cache
        return loadLocalPack(pack);
    }

    private byte[] fetchRemotePack(ResourcePackInfo pack) {
        try {
            reportProgress(-1, "Downloading " + pack.name + "...");

            // Use WebFs fetch mechanism
            byte[] data = WebFs.fetchSync(pack.sourceLocation);

            if (data == null || data.length == 0) {
                pack.setErrorMessage("Download failed or empty response");
                return null;
            }

            return data;
        } catch (Exception e) {
            pack.setErrorMessage("Download failed: " + e.getMessage());
            return null;
        }
    }

    private boolean extractPackToVfs(ResourcePackInfo pack, byte[] zipData) {
        int extracted = 0;
        int overridden = 0;
        List<String> extractedPaths = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String entryName = entry.getName();

                // Skip pack.mcmeta at root level
                if (entryName.equals("pack.mcmeta") || entryName.equals("META-INF/")) {
                    zis.closeEntry();
                    continue;
                }

                // Only extract assets
                if (!entryName.startsWith("assets/")) {
                    zis.closeEntry();
                    continue;
                }

                String vfsPath = "/" + entryName;

                // Check if this resource already exists (will be overridden)
                if (WebFs.exists(vfsPath)) {
                    overridden++;
                }

                // Read and write entry
                byte[] entryData = readAllBytes(zis);
                WebFs.writeBytes(vfsPath, entryData);
                extractedPaths.add(vfsPath);
                extracted++;

                // Register in override map
                String resourcePath = entryName.substring("assets/".length());
                int slashIdx = resourcePath.indexOf('/');
                if (slashIdx > 0) {
                    String namespace = resourcePath.substring(0, slashIdx);
                    String path = resourcePath.substring(slashIdx + 1);
                    String key = namespace + ":" + path;
                    resourceOverrides.put(key, pack.id);
                }

                zis.closeEntry();
            }
        } catch (IOException e) {
            pack.setErrorMessage("Failed to extract pack: " + e.getMessage());
            return false;
        }

        // Register extracted paths with WebFs for lifecycle management
        WebFs.registerPackExtractedPaths(pack.id, extractedPaths);

        WebFs.log("ResourcePackManager: Extracted " + extracted + " files from " + pack.name +
                   " (" + overridden + " overridden)");
        pack.setLoaded(true);
        pack.setErrorMessage(null);
        return true;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private PackMetadata parsePackMetadata(byte[] zipData) {
        PackMetadata metadata = new PackMetadata();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("pack.mcmeta")) {
                    byte[] data = readAllBytes(zis);
                    String content = new String(data, StandardCharsets.UTF_8);

                    // Parse nested pack.mcmeta structure: { "pack": { "name": "...", "description": "...", "pack_format": N } }
                    // First extract the "pack" object content
                    int packStart = content.indexOf("\"pack\"");
                    if (packStart >= 0) {
                        // Find the opening brace after "pack"
                        int braceStart = content.indexOf('{', packStart);
                        if (braceStart >= 0) {
                            // Find matching closing brace (simple approach: count braces)
                            int braceCount = 1;
                            int pos = braceStart + 1;
                            while (pos < content.length() && braceCount > 0) {
                                char c = content.charAt(pos);
                                if (c == '{') braceCount++;
                                else if (c == '}') braceCount--;
                                pos++;
                            }
                            if (braceCount == 0) {
                                String packContent = content.substring(braceStart + 1, pos - 1);
                                metadata.name = extractJsonString(packContent, "name");
                                metadata.description = extractJsonString(packContent, "description");
                                metadata.packFormat = extractJsonInt(packContent, "pack_format");
                            }
                        }
                    }
                    break;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            // Ignore, use defaults
        }

        return metadata;
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private int extractJsonInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*";
        int start = json.indexOf(pattern);
        if (start < 0) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c < '0' || c > '9') break;
            end++;
        }
        String num = json.substring(start, end);
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addPackInternal(ResourcePackInfo pack) {
        synchronized (allPacks) {
            allPacks.put(pack.id, pack);

            if (pack.isEnabled()) {
                // Insert at correct position based on type
                int insertPos = enabledPacks.size();
                if ("builtin".equals(pack.sourceType)) {
                    // Builtin always at the end
                    insertPos = enabledPacks.size();
                } else if ("remote".equals(pack.sourceType)) {
                    // Remote goes before builtin
                    insertPos = enabledPacks.size();
                    for (int i = 0; i < enabledPacks.size(); i++) {
                        if ("builtin".equals(enabledPacks.get(i).sourceType)) {
                            insertPos = i;
                            break;
                        }
                    }
                }
                // Local packs go to the front (highest priority)

                // Find correct position for local packs
                if ("local".equals(pack.sourceType)) {
                    int firstNonLocal = 0;
                    for (int i = 0; i < enabledPacks.size(); i++) {
                        if (!"local".equals(enabledPacks.get(i).sourceType)) {
                            firstNonLocal = i;
                            break;
                        }
                    }
                    enabledPacks.add(firstNonLocal, pack);
                } else {
                    enabledPacks.add(insertPos, pack);
                }
            }

            rebuildOverrideMap();
        }
    }

    private void rebuildEnabledList() {
        enabledPacks.clear();

        // Sort by type priority: local > remote > builtin
        List<ResourcePackInfo> locals = new ArrayList<>();
        List<ResourcePackInfo> remotes = new ArrayList<>();
        ResourcePackInfo builtin = null;

        for (ResourcePackInfo pack : allPacks.values()) {
            if (!pack.isEnabled()) continue;

            switch (pack.sourceType) {
                case "local":
                    locals.add(pack);
                    break;
                case "remote":
                    remotes.add(pack);
                    break;
                case "builtin":
                    builtin = pack;
                    break;
            }
        }

        enabledPacks.addAll(locals);
        enabledPacks.addAll(remotes);
        if (builtin != null) {
            enabledPacks.add(builtin);
        }

        rebuildOverrideMap();
    }

    private void rebuildOverrideMap() {
        resourceOverrides.clear();

        // Process from lowest to highest priority, so higher priority overwrites
        // But we iterate enabledPacks which is already ordered high-to-low
        // So we process in reverse
        for (int i = enabledPacks.size() - 1; i >= 0; i--) {
            ResourcePackInfo pack = enabledPacks.get(i);
            registerPackResources(pack);
        }
    }

    private void registerPackResources(ResourcePackInfo pack) {
        if (!pack.isLoaded()) return;

        // For loaded packs, scan their resources and add to override map
        // This is handled during extraction, but for builtin we need to scan
        if ("builtin".equals(pack.sourceType)) {
            // Scan /assets directory for builtin resources
            if (WebFs.isDirectory("/assets")) {
                scanResourcesForPack("/", pack.id);
            }
        }
    }

    private void scanResourcesForPack(String basePath, String packId) {
        String[] children = WebFs.listPaths(basePath);
        if (children == null) return;

        for (String child : children) {
            String childPath = basePath.endsWith("/") ? basePath + child : basePath + "/" + child;

            if (WebFs.isDirectory(childPath)) {
                scanResourcesForPack(childPath, packId);
            } else if (childPath.startsWith("/assets/")) {
                String resourcePath = childPath.substring("/assets/".length());
                int slashIdx = resourcePath.indexOf('/');
                if (slashIdx > 0) {
                    String namespace = resourcePath.substring(0, slashIdx);
                    String path = resourcePath.substring(slashIdx + 1);
                    String key = namespace + ":" + path;
                    resourceOverrides.put(key, packId);
                }
            }
        }
    }

    private void rebuildManifest() {
        this.manifest = ResourceManifest.build();
    }

    private void reportProgress(float progress, String status) {
        if (progressCallback != null) {
            progressCallback.onProgress(progress, status);
        }
    }

    private String extractPath(Object resourceLocation) {
        // ResourceLocation.toString() or getPath() method
        // This is a simplified extraction
        try {
            return resourceLocation.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the current resource manifest.
     */
    public ResourceManifest getManifest() {
        return manifest;
    }

    /**
     * Check if currently loading.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Get resource override statistics.
     */
    public int getOverrideCount() {
        return resourceOverrides.size();
    }

    private static class PackMetadata {
        String name;
        String description;
        int packFormat;
    }

    private ResourcePackManager(String s) {
        throw new AssertionError("Use constructor");
    }
}