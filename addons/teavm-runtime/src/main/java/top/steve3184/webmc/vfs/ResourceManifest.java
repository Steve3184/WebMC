package top.steve3184.webmc.vfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Resource manifest and dependency resolver for the WebMC browser runtime.
 *
 * <p>This class indexes all resources in the WebFs virtual filesystem and tracks
 * dependencies between resources (e.g., .json models reference textures, .mcmeta
 * files are metadata for their parent resources).</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   // Build the manifest after WebFs.preload()
 *   ResourceManifest manifest = ResourceManifest.build();
 *
 *   // Check if a resource exists
 *   boolean hasTexture = manifest.hasResource("minecraft", "textures/block/dirt.png");
 *
 *   // Get all dependencies of a resource
 *   Set&lt;ResourceLocation&gt; deps = manifest.getDependencies(
 *       ResourceLocation.parse("minecraft:block/stone"));
 *
 *   // List all resources of a type
 *   List&lt;ResourceLocation&gt; allModels = manifest.listResources(
 *       "minecraft", ResourceType.MODEL);
 * </pre>
 */
public final class ResourceManifest {

    /** Resource type categories for dependency analysis. */
    public enum ResourceType {
        MODEL(".json"),
        TEXTURE(".png"),
        SOUND(".ogg"),
        LANG(".json"),
        SHADER(".glsl"),
        SHADER_JSON(".json"),
        ATLAS(".png"),
        OTHER("");

        private final String extension;

        ResourceType(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    private final Map<String, NamespaceIndex> namespaces;
    private final List<ResourceLocation> allResources;
    private volatile boolean built;

    private ResourceManifest() {
        this.namespaces = new HashMap<>();
        this.allResources = new ArrayList<>();
        this.built = false;
    }

    /**
     * Build a new manifest by scanning the WebFs virtual filesystem.
     * This operation is synchronous and may take time for large resource sets.
     *
     * @return A new ResourceManifest with all indexed resources
     */
    public static ResourceManifest build() {
        return build(true);
    }

    /**
     * Build a new manifest by scanning the WebFs virtual filesystem.
     *
     * @param resolveDependencies If true, analyze dependencies between resources
     * @return A new ResourceManifest with all indexed resources
     */
    public static ResourceManifest build(boolean resolveDependencies) {
        ResourceManifest manifest = new ResourceManifest();
        manifest.scanAll(resolveDependencies);
        return manifest;
    }

    /**
     * Scan the WebFs for all resources.
     */
    private void scanAll(boolean resolveDependencies) {
        if (!WebFs.isDirectory("/assets")) {
            return;
        }

        String[] namespaces = WebFs.listPaths("/assets");
        if (namespaces == null) return;

        for (String namespace : namespaces) {
            if (!ResourceLocation.isValidNamespace(namespace)) continue;
            scanNamespace(namespace, resolveDependencies);
        }
    }

    /**
     * Scan a specific namespace for resources.
     */
    private void scanNamespace(String namespace, boolean resolveDependencies) {
        NamespaceIndex nsIndex = new NamespaceIndex(namespace);
        this.namespaces.put(namespace, nsIndex);

        // Scan different resource types
        scanDirectory(namespace, "textures", nsIndex.textures, ResourceType.TEXTURE);
        scanDirectory(namespace, "models", nsIndex.models, ResourceType.MODEL);
        scanDirectory(namespace, "sounds", nsIndex.sounds, ResourceType.SOUND);
        scanDirectory(namespace, "lang", nsIndex.langs, ResourceType.LANG);
        scanDirectory(namespace, "shaders", nsIndex.shaders, ResourceType.SHADER);
        scanDirectory(namespace, "atlases", nsIndex.atlases, ResourceType.ATLAS);

        // Scan all resources recursively for other types
        scanRecursive(namespace, "/assets/" + namespace, nsIndex);

        // Add all discovered resources to the global list
        synchronized (this.allResources) {
            this.allResources.addAll(nsIndex.allResources);
        }

        // Resolve dependencies if requested
        if (resolveDependencies) {
            resolveDependencies(nsIndex);
        }

        this.built = true;
    }

    /**
     * Scan a specific subdirectory for resources of a given type.
     */
    private void scanDirectory(String namespace, String subDir,
                               Map<String, ResourceLocation> target,
                               ResourceType type) {
        String path = "/assets/" + namespace + "/" + subDir;
        if (!WebFs.isDirectory(path)) return;

        scanRecursiveFiles(namespace, path, subDir + "/", target, type);
    }

    /**
     * Recursively scan files starting from a base path.
     */
    private void scanRecursiveFiles(String namespace, String basePath, String relativePrefix,
                                     Map<String, ResourceLocation> target, ResourceType type) {
        String[] children = WebFs.listPaths(basePath);
        if (children == null) return;

        for (String child : children) {
            String childPath = basePath.endsWith("/") ? basePath + child : basePath + "/" + child;

            if (WebFs.isDirectory(childPath)) {
                scanRecursiveFiles(namespace, childPath, relativePrefix + child + "/",
                                   target, type);
            } else if (child.endsWith(type.getExtension())) {
                String relativePath = relativePrefix + child;
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, relativePath);
                target.put(relativePath, loc);
                synchronized (this.allResources) {
                    this.allResources.add(loc);
                }
            }
        }
    }

    /**
     * Recursively scan all files for a namespace index.
     */
    private void scanRecursive(String namespace, String basePath, NamespaceIndex nsIndex) {
        String[] children = WebFs.listPaths(basePath);
        if (children == null) return;

        for (String child : children) {
            String childPath = basePath.endsWith("/") ? basePath + child : basePath + "/" + child;

            if (WebFs.isDirectory(childPath)) {
                scanRecursive(namespace, childPath, nsIndex);
            } else {
                // Add to all resources
                String relativePath = childPath.substring(("/assets/" + namespace + "/").length());
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, relativePath);
                nsIndex.allResources.add(loc);

                // Categorize by type
                if (child.endsWith(".json")) {
                    if (child.contains("/shapes/") || child.contains("/elements/")) {
                        // model json
                    } else if (child.endsWith(".mcmeta")) {
                        // metadata file - will be handled specially
                    } else {
                        nsIndex.models.put(relativePath, loc);
                    }
                } else if (child.endsWith(".png")) {
                    nsIndex.textures.put(relativePath, loc);
                } else if (child.endsWith(".ogg")) {
                    nsIndex.sounds.put(relativePath, loc);
                }
            }
        }
    }

    /**
     * Resolve dependencies between resources.
     * For example, .json models reference textures, .mcmeta files are metadata for their parent.
     */
    private void resolveDependencies(NamespaceIndex nsIndex) {
        // Create a reverse index for quick lookup
        Map<String, ResourceLocation> textureMap = new HashMap<>();
        for (Map.Entry<String, ResourceLocation> entry : nsIndex.textures.entrySet()) {
            String key = entry.getKey().replace(".png", "");
            textureMap.put(key, entry.getValue());
        }

        // For each model, find its texture dependencies
        for (Map.Entry<String, ResourceLocation> modelEntry : nsIndex.models.entrySet()) {
            String modelPath = modelEntry.getKey();
            ResourceLocation modelLoc = modelEntry.getValue();

            Set<ResourceLocation> deps = new HashSet<>();

            // Try to read model JSON and extract texture references
            byte[] modelData = WebFs.readBytes("/assets/" + modelLoc.getNamespace() + "/" + modelPath);
            if (modelData != null) {
                String content = new String(modelData, java.nio.charset.StandardCharsets.UTF_8);

                // Extract texture references from model JSON
                // Common patterns: "textures": { ... }, "layer0": "..."
                extractTextureDeps(content, textureMap, deps);
            }

            // Add .mcmeta file as a dependency if it exists
            String metaPath = "/assets/" + modelLoc.getNamespace() + "/" + modelPath + ".mcmeta";
            if (WebFs.exists(metaPath)) {
                ResourceLocation metaLoc = ResourceLocation.fromNamespaceAndPath(
                    modelLoc.getNamespace(), modelPath + ".mcmeta");
                deps.add(metaLoc);
            }

            if (!deps.isEmpty()) {
                nsIndex.dependencies.put(modelLoc, deps);
            }
        }
    }

    /**
     * Extract texture dependencies from model JSON content.
     */
    private void extractTextureDeps(String content, Map<String, ResourceLocation> textureMap,
                                    Set<ResourceLocation> deps) {
        // Simple pattern matching for texture references in JSON
        // This handles common patterns like "textures": { "particle": "minecraft:block/dirt" }
        String[] lines = content.split("\n");

        for (String line : lines) {
            // Look for texture path patterns
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String value = extractQuotedValue(line.substring(colonIdx + 1));
                if (value != null && value.contains("/")) {
                    ResourceLocation texLoc = ResourceLocation.tryParse(value);
                    if (texLoc != null) {
                        // Check if this texture exists in our index
                        String texPath = texLoc.getPath();
                        if (textureMap.containsKey(texPath)) {
                            deps.add(textureMap.get(texPath));
                        }
                    }
                }
            }
        }
    }

    /**
     * Extract a quoted string value from JSON.
     */
    private String extractQuotedValue(String s) {
        s = s.trim();
        if (s.startsWith("\"")) {
            int endQuote = s.indexOf('"', 1);
            if (endQuote > 0) {
                return s.substring(1, endQuote);
            }
        }
        return null;
    }

    /**
     * Check if a resource exists in the manifest.
     */
    public boolean hasResource(String namespace, String path) {
        NamespaceIndex ns = namespaces.get(namespace);
        if (ns == null) return false;
        return ns.allResources.contains(
            ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Check if a resource exists in the manifest.
     */
    public boolean hasResource(ResourceLocation location) {
        return hasResource(location.getNamespace(), location.getPath());
    }

    /**
     * Get all resources of a specific type.
     */
    public List<ResourceLocation> listResources(String namespace, ResourceType type) {
        NamespaceIndex ns = namespaces.get(namespace);
        if (ns == null) return Collections.emptyList();

        switch (type) {
            case MODEL: return new ArrayList<>(ns.models.values());
            case TEXTURE: return new ArrayList<>(ns.textures.values());
            case SOUND: return new ArrayList<>(ns.sounds.values());
            case LANG: return new ArrayList<>(ns.langs.values());
            case SHADER: return new ArrayList<>(ns.shaders.values());
            case ATLAS: return new ArrayList<>(ns.atlases.values());
            default: return new ArrayList<>(ns.allResources);
        }
    }

    /**
     * Get all resources in a namespace.
     */
    public List<ResourceLocation> listResources(String namespace) {
        NamespaceIndex ns = namespaces.get(namespace);
        if (ns == null) return Collections.emptyList();
        return new ArrayList<>(ns.allResources);
    }

    /**
     * Get all namespaces in this manifest.
     */
    public Set<String> getNamespaces() {
        return new HashSet<>(namespaces.keySet());
    }

    /**
     * Get all dependencies for a resource.
     * Returns an empty set if the resource has no tracked dependencies.
     */
    public Set<ResourceLocation> getDependencies(ResourceLocation location) {
        NamespaceIndex ns = namespaces.get(location.getNamespace());
        if (ns == null) return Collections.emptySet();

        Set<ResourceLocation> deps = ns.dependencies.get(location);
        return deps != null ? deps : Collections.emptySet();
    }

    /**
     * Get the total count of indexed resources.
     */
    public int getResourceCount() {
        synchronized (this.allResources) {
            return this.allResources.size();
        }
    }

    /**
     * Check if the manifest has been built.
     */
    public boolean isBuilt() {
        return this.built;
    }

    /**
     * Print diagnostic info about the manifest.
     */
    public void dump() {
        System.out.println("[ResourceManifest] === DUMP START ===");
        System.out.println("[ResourceManifest] Namespaces: " + namespaces.keySet());
        System.out.println("[ResourceManifest] Total resources: " + getResourceCount());

        for (Map.Entry<String, NamespaceIndex> entry : namespaces.entrySet()) {
            NamespaceIndex ns = entry.getValue();
            System.out.println("[ResourceManifest] Namespace " + entry.getKey() + ":");
            System.out.println("  - Textures: " + ns.textures.size());
            System.out.println("  - Models: " + ns.models.size());
            System.out.println("  - Sounds: " + ns.sounds.size());
            System.out.println("  - Lang: " + ns.langs.size());
            System.out.println("  - Shaders: " + ns.shaders.size());
            System.out.println("  - Atlases: " + ns.atlases.size());
            System.out.println("  - Total: " + ns.allResources.size());
        }

        System.out.println("[ResourceManifest] === DUMP END ===");
    }

    /**
     * Namespace-level resource index.
     */
    private static class NamespaceIndex {
        final String namespace;
        final Map<String, ResourceLocation> textures = new HashMap<>();
        final Map<String, ResourceLocation> models = new HashMap<>();
        final Map<String, ResourceLocation> sounds = new HashMap<>();
        final Map<String, ResourceLocation> langs = new HashMap<>();
        final Map<String, ResourceLocation> shaders = new HashMap<>();
        final Map<String, ResourceLocation> atlases = new HashMap<>();
        final Set<ResourceLocation> allResources = new HashSet<>();
        final Map<ResourceLocation, Set<ResourceLocation>> dependencies = new HashMap<>();

        NamespaceIndex(String namespace) {
            this.namespace = namespace;
        }
    }

    private ResourceManifest(String s) {
        throw new AssertionError("Use build() factory method");
    }
}
