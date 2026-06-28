package top.steve3184.webmc.vfs;

import java.util.UUID;

/**
 * Resource pack information and metadata holder.
 * Represents a single resource pack in the loading stack.
 *
 * <p>Each pack has a unique ID, display name, description, source type, and priority.
 * Source types:</p>
 * <ul>
 *   <li><b>builtin</b> - Bundled vanilla assets (lowest priority)</li>
 *   <li><b>local</b> - User-selected .zip/.mcpack files (highest priority)</li>
 *   <li><b>remote</b> - Resource packs downloaded from HTTP URLs (medium priority)</li>
 * </ul>
 *
 * <p>Priority order (user-configurable for same-type packs):</p>
 * <ol>
 *   <li>Local packs (in user-defined order)</li>
 *   <li>Remote packs (in user-defined order)</li>
 *   <li>Built-in pack (always lowest, single instance)</li>
 * </ol>
 */
public final class ResourcePackInfo {

    /** Unique identifier for this pack */
    public final String id;

    /** Display name of the pack */
    public String name;

    /** Description shown in UI */
    public String description;

    /** Source type: "builtin", "local", "remote" */
    public final String sourceType;

    /** URL for remote packs, file path for local packs, empty for builtin */
    public final String sourceLocation;

    /** User-defined priority (higher = loaded first, overrides lower) */
    private int priority;

    /** Whether this pack is currently enabled */
    private boolean enabled;

    /** Pack format version (Minecraft version compatibility) */
    public final int packFormat;

    /** Estimated file size in bytes */
    public long estimatedSize;

    /** Loaded status */
    private volatile boolean loaded;

    /** Loading progress (0-100) */
    private volatile int loadProgress;

    /** Error message if loading failed */
    private String errorMessage;

    /** SHA-1 hash of pack file (for cache validation) */
    private String hash;

    /** Last modified timestamp */
    private long lastModified;

    /** Pack format compatibility with current Minecraft version */
    private boolean compatible = true;

    /** Lock object for thread safety */
    private final Object lock = new Object();

    public ResourcePackInfo(String id, String name, String description,
                            String sourceType, String sourceLocation,
                            int packFormat, long estimatedSize) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceLocation = sourceLocation;
        this.packFormat = packFormat;
        this.estimatedSize = estimatedSize;
        this.enabled = true;
        this.loaded = false;
        this.loadProgress = 0;
        this.priority = 0;
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Create a built-in pack (vanilla assets).
     */
    public static ResourcePackInfo builtin(String name, String description, int packFormat) {
        return new ResourcePackInfo(
            "builtin_vanilla",
            name,
            description,
            "builtin",
            "",
            packFormat,
            0
        );
    }

    /**
     * Create a local pack from file selection.
     */
    public static ResourcePackInfo local(UUID id, String name, String filePath,
                                         int packFormat, long fileSize) {
        return new ResourcePackInfo(
            "local_" + id.toString(),
            name,
            "Local resource pack: " + name,
            "local",
            filePath,
            packFormat,
            fileSize
        );
    }

    /**
     * Create a remote pack from URL.
     */
    public static ResourcePackInfo remote(UUID id, String name, String url,
                                          int packFormat, long fileSize) {
        return new ResourcePackInfo(
            "remote_" + id.toString(),
            name,
            "Remote resource pack: " + name,
            "remote",
            url,
            packFormat,
            fileSize
        );
    }

    /**
     * Create a pack from a serialized data object (e.g., from localStorage).
     */
    public static ResourcePackInfo fromData(String id, String name, String description,
                                            String sourceType, String sourceLocation,
                                            int packFormat, long estimatedSize,
                                            boolean enabled, int priority, String hash) {
        ResourcePackInfo pack = new ResourcePackInfo(
            id, name, description, sourceType, sourceLocation, packFormat, estimatedSize
        );
        pack.enabled = enabled;
        pack.priority = priority;
        pack.hash = hash;
        return pack;
    }

    // ----- Getters and setters -----

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lastModified = System.currentTimeMillis();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
        if (loaded) {
            this.loadProgress = 100;
        }
    }

    public int getLoadProgress() {
        return loadProgress;
    }

    public void setLoadProgress(int progress) {
        this.loadProgress = Math.max(0, Math.min(100, progress));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * Check if this pack is compatible with the current Minecraft version.
     */
    public boolean isCompatible() {
        return compatible;
    }

    /**
     * Set the pack's compatibility status.
     */
    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.lastModified = System.currentTimeMillis();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getLastModified() {
        return lastModified;
    }

    /**
     * Check if this pack is compatible with a given pack format.
     */
    public boolean isCompatible(int targetFormat) {
        // Minecraft 1.20.x uses format 15
        // Allow some flexibility for similar versions
        return Math.abs(packFormat - targetFormat) <= 2;
    }

    /**
     * Get human-readable size string.
     */
    public String getFormattedSize() {
        if (estimatedSize <= 0) return "Unknown";
        if (estimatedSize < 1024) return estimatedSize + " B";
        if (estimatedSize < 1024 * 1024) return String.format("%.1f KB", estimatedSize / 1024.0);
        if (estimatedSize < 1024 * 1024 * 1024) return String.format("%.1f MB", estimatedSize / (1024.0 * 1024));
        return String.format("%.2f GB", estimatedSize / (1024.0 * 1024 * 1024));
    }

    // ----- Serialization -----

    /**
     * Serialize pack info to a JSON-friendly object.
     */
    public PackData toData() {
        return new PackData(
            id, name, description, sourceType, sourceLocation,
            packFormat, estimatedSize, enabled, priority, hash, compatible
        );
    }

    /**
     * Pack data record for serialization.
     */
    public record PackData(
        String id,
        String name,
        String description,
        String sourceType,
        String sourceLocation,
        int packFormat,
        long estimatedSize,
        boolean enabled,
        int priority,
        String hash,
        boolean compatible
    ) {
        public ResourcePackInfo toPackInfo() {
            ResourcePackInfo pack = ResourcePackInfo.fromData(
                id, name, description, sourceType, sourceLocation,
                packFormat, estimatedSize, enabled, priority, hash
            );
            pack.setCompatible(compatible);
            return pack;
        }
    }

    // ----- Object methods -----

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResourcePackInfo)) return false;
        return id.equals(((ResourcePackInfo) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ResourcePackInfo{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", sourceType='" + sourceType + '\'' +
               ", enabled=" + enabled +
               ", loaded=" + loaded +
               ", priority=" + priority +
               ", compatible=" + compatible +
               '}';
    }
}