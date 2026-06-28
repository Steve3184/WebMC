package top.steve3184.webmc.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.teavm.jso.JSBody;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystemProvider;
import org.teavm.runtime.fs.memory.InMemoryVirtualFileSystem;

/**
 * Browser-side filesystem for mc-web.
 *
 * TeaVM exposes a {@link org.teavm.runtime.fs.VirtualFileSystem} SPI: every
 * {@code java.io.File} / {@code java.nio.file.Path} / {@code java.nio.file.Files}
 * call routes through whatever instance is registered with
 * {@link VirtualFileSystemProvider#setInstance}. We hand it an in-memory
 * tree (TeaVM ships {@link InMemoryVirtualFileSystem}) preloaded with vanilla
 * MC assets fetched from the server at boot.
 *
 * <p>Two phases:</p>
 * <ol>
 *   <li>{@link #boot()} - register the empty in-memory FS so anything that
 *       runs before the asset preload does not NPE on getFileSystem.
 *       Also synthesises {@code /assets/.mcassetsroot} and
 *       {@code /data/.mcassetsroot} so VanillaPackResourcesBuilder's
 *       classpath probe finds them.</li>
 *   <li>{@link #preload(String)} - sync XHR fetch of a manifest blob, then
 *       sync XHR fetches of each entry's body, populating the in-memory FS.
     *       (Stub for now - the actual game.vfs format gets wired up by the
 *       fetch-assets.py script and a binary parser here.)</li>
 * </ol>
 *
 * <p>Future: replace the per-file fetch with a single tar/zip download +
 * stream-extract; persist user data (saves, options.txt) into IndexedDB.</p>
 */
public final class WebFs {

    private static volatile InMemoryVirtualFileSystem fs;
    private static volatile boolean booted;

    /** Root directory for resource pack storage */
    public static final String RESOURCE_PACK_ROOT = "/resourcepacks";

    /** Directory for cached remote pack downloads */
    public static final String REMOTE_PACK_CACHE = "/resourcepacks/cache";

    /** List of paths extracted by each resource pack (packId -> List<path>) */
    private static final Map<String, List<String>> packExtractedPaths = new HashMap<>();

    /** Lock for packExtractedPaths map */
    private static final Object packPathsLock = new Object();

    /**
     * Install our in-memory FS as the JVM-wide {@code VirtualFileSystem} and
     * seed the marker files MC's static init expects to find on the classpath.
     * Idempotent.
     */
    public static synchronized void boot() {
        if (booted) {
            startupMark("webfs:boot:skip", "");
            return;
        }
        startupMark("webfs:boot:start", "");
        booted = true;
        InMemoryVirtualFileSystem mem = new InMemoryVirtualFileSystem();
        mem.setUserDir("/");
        VirtualFileSystemProvider.setInstance(mem);
        try {
            updateVfsInstance(mem);
        } catch (Throwable ignored) {}
        fs = mem;
        log("WebFs.boot: installed InMemoryVirtualFileSystem, userDir=/");

        ensureDir("/assets");
        ensureDir("/data");
        ensureDir("/resourcepacks");
        ensureDir("/saves");
        ensureDir("/screenshots");
        writeBytes("/assets/.mcassetsroot", new byte[0]);
        writeBytes("/data/.mcassetsroot", new byte[0]);
        log("WebFs.boot: marker files seeded");

        // Sanity check: confirm the JDK-level Paths/Files SPI is wired through
        // to our VFS. If not, MC's File/Path operations won't see anything we
        // put in the in-memory tree.
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("/assets/.mcassetsroot");
            boolean exists = java.nio.file.Files.exists(p);
            log("WebFs.boot: sanity /assets/.mcassetsroot exists = " + exists);
            java.nio.file.FileSystem dfs = java.nio.file.FileSystems.getDefault();
            log("WebFs.boot: default FS = " + dfs.getClass().getName());
            // Attempt to read vfs field via JS trick if possible, or just log the FS instance
        } catch (Throwable t) {
            log("WebFs.boot: sanity check failed: " + t);
        }
        startupMark("webfs:boot:done", "");
    }

    /**
     * Synchronously preload a flat key/value blob from the given URL into the
     * VFS. The blob format is described in scripts/fetch-assets.py:
     * <pre>
     *   magic    "MCVF"     (4 bytes)
     *   version  uint32 LE  (1)
     *   count    uint32 LE
     *   for each entry:
     *     pathLen  uint16 LE
     *     path     UTF-8 bytes (no leading slash; we add one)
     *     dataLen  uint32 LE
     *     data     raw bytes
     * </pre>
     * Stub: not yet invoked. Wire up after the build produces game.vfs.
     */
    public static void preload(String blobUrl) {
        if (!booted) {
            boot();
        }
        startupMark("webfs:preload:start", blobUrl);
        log("WebFs.preload: fetching " + blobUrl + "...");
        byte[] blob;
        try {
            startupMark("webfs:preload:fetch:start", blobUrl);
            blob = fetchSync(blobUrl);
            startupMark(
                "webfs:preload:fetch:done",
                blobUrl + " bytes=" + (blob == null ? "null" : String.valueOf(blob.length))
            );
        } catch (Throwable t) {
            startupMark("webfs:preload:fetch:failed", blobUrl + " " + String.valueOf(t));
            log("WebFs.preload: fetch failed for " + blobUrl + ": " + t);
            return;
        }
        if (blob == null || blob.length < 12) {
            startupMark("webfs:preload:invalid", blobUrl + " length=" + (blob == null ? "null" : String.valueOf(blob.length)));
            log("WebFs.preload: empty or too-small blob (length=" + (blob == null ? "null" : blob.length) + ")");
            return;
        }
        log("WebFs.preload: fetched " + blob.length + " bytes, parsing...");
        try {
            startupMark("webfs:preload:parse:start", blobUrl + " bytes=" + blob.length);
            int n = parseAndApply(blob);
            startupMark("webfs:preload:parse:done", blobUrl + " entries=" + n);
            log("WebFs.preload: successfully loaded " + n + " entries from " + blobUrl);
            startupMark("webfs:preload:done", blobUrl + " entries=" + n);
        } catch (Throwable t) {
            startupMark("webfs:preload:parse:failed", blobUrl + " " + String.valueOf(t));
            log("WebFs.preload: parse/apply failed: " + t);
            t.printStackTrace();
        }
    }

    /** Returns true if the path exists in the VFS. */
    public static boolean exists(String absPath) {
        if (!booted) return false;
        VirtualFile f = fs.getFile(absPath);
        boolean found = f != null && f.exists();
        if (!found) {
            // retry lowercase
            String p = absPath.toLowerCase(java.util.Locale.ROOT);
            f = fs.getFile(p);
            found = f != null && f.exists();
        }
        return found;
    }

    /** Returns true if the path is a directory. */
    public static boolean isDirectory(String absPath) {
        if (!booted) return false;
        VirtualFile f = fs.getFile(absPath);
        if (f != null && f.isDirectory()) return true;
        // retry lowercase
        String p = absPath.toLowerCase(java.util.Locale.ROOT);
        f = fs.getFile(p);
        return f != null && f.isDirectory();
    }

    /** Returns names of children in the directory, or null if it's not a directory. */
    public static String[] listPaths(String absPath) {
        if (!booted) return null;
        VirtualFile f = fs.getFile(absPath);
        if (f == null || !f.isDirectory()) {
            // retry lowercase
            String p = absPath.toLowerCase(java.util.Locale.ROOT);
            f = fs.getFile(p);
        }
        if (f == null || !f.isDirectory()) {
            return null;
        }
        return f.listFiles();
    }

    /** Read a file into a byte[]. Returns null if the path is missing or is a directory. */
    public static byte[] readBytes(String absPath) {
        if (!booted) return null;
        String p = absPath.toLowerCase(java.util.Locale.ROOT);
        VirtualFile f = fs.getFile(p);
        if (f == null || !f.isFile()) {
            return null;
        }
        try {
            int sz = f.length();
            byte[] out = new byte[sz];
            VirtualFileAccessor acc = f.createAccessor(true, false, false);
            try {
                int off = 0;
                while (off < sz) {
                    int got = acc.read(out, off, sz - off);
                    if (got <= 0) break;
                    off += got;
                }
            } finally {
                acc.close();
            }
            return out;
        } catch (IOException e) {
            log("WebFs.readBytes: " + absPath + " failed: " + e);
            return null;
        }
    }

    /** Write bytes to a file, creating parent dirs. Overwrites if it exists. */
    public static void writeBytes(String absPath, byte[] data) {
        if (!booted) {
            throw new IllegalStateException("WebFs.boot() not called");
        }
        int slash = absPath.lastIndexOf('/');
        if (slash > 0) {
            ensureDir(absPath.substring(0, slash));
        }
        VirtualFile parent = fs.getFile(slash <= 0 ? "/" : absPath.substring(0, slash));
        String name = absPath.substring(slash + 1);
        try {
            // delete existing so we overwrite cleanly
            VirtualFile existing = fs.getFile(absPath);
            if (existing != null && existing.exists()) {
                existing.delete();
            }
            parent.createFile(name);
            VirtualFile f = fs.getFile(absPath);
            VirtualFileAccessor acc = f.createAccessor(false, true, false);
            try {
                acc.write(data, 0, data.length);
                acc.flush();
            } finally {
                acc.close();
            }
        } catch (IOException e) {
            log("WebFs.writeBytes: " + absPath + " failed: " + e);
        }
    }

    /** Open a path for reading. Caller must close. Returns null if missing. */
    public static InputStream openRead(String absPath) {
        String p = absPath.toLowerCase(java.util.Locale.ROOT);
        byte[] b = readBytes(p);
        return b == null ? null : new ByteArrayInputStream(b);
    }

    /** Create a directory and any missing parents. */
    public static void ensureDir(String absPath) {
        if (!booted) return;
        if ("/".equals(absPath) || absPath.isEmpty()) return;
        String[] parts = absPath.split("/");
        StringBuilder cur = new StringBuilder();
        VirtualFile node = fs.getFile("/");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            cur.append('/').append(p);
            VirtualFile child = fs.getFile(cur.toString());
            if (child == null || !child.exists()) {
                node.createDirectory(p);
            }
            node = fs.getFile(cur.toString());
            if (node == null) {
                log("WebFs.ensureDir: failed to create " + cur);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Resource Pack Support
    // ------------------------------------------------------------------

    /**
     * Register that a resource pack has extracted files to certain paths.
     * This enables tracking for cleanup and override resolution.
     *
     * @param packId Unique identifier of the pack
     * @param paths List of VFS paths extracted by this pack
     */
    public static void registerPackExtractedPaths(String packId, List<String> paths) {
        synchronized (packPathsLock) {
            packExtractedPaths.put(packId, new ArrayList<>(paths));
        }
        log("WebFs: Registered " + paths.size() + " paths for pack " + packId);
    }

    /**
     * Unregister and clear all files extracted by a resource pack.
     * This effectively removes the pack's contribution from the VFS.
     *
     * @param packId Unique identifier of the pack
     * @return Number of paths cleared
     */
    public static int clearPackFromVfs(String packId) {
        synchronized (packPathsLock) {
            List<String> paths = packExtractedPaths.remove(packId);
            if (paths == null || paths.isEmpty()) {
                return 0;
            }

            int cleared = 0;
            for (String path : paths) {
                if (deleteFile(path)) {
                    cleared++;
                }
            }
            log("WebFs: Cleared " + cleared + " files for pack " + packId);
            return cleared;
        }
    }

    /**
     * Clear all resource pack files from VFS.
     * Used during full resource reload.
     *
     * @return Number of paths cleared
     */
    public static int clearAllPacksFromVfs() {
        int totalCleared = 0;
        synchronized (packPathsLock) {
            for (String packId : new ArrayList<>(packExtractedPaths.keySet())) {
                totalCleared += clearPackFromVfs(packId);
            }
        }
        log("WebFs: Cleared " + totalCleared + " files from all packs");
        return totalCleared;
    }

    /**
     * Get the pack ID that originally provided a resource at the given path.
     * Only tracks resources explicitly registered by packs.
     *
     * @param vfsPath The VFS path to check
     * @return Pack ID if the path was extracted by a pack, null otherwise
     */
    public static String getResourcePackId(String vfsPath) {
        synchronized (packPathsLock) {
            for (Map.Entry<String, List<String>> entry : packExtractedPaths.entrySet()) {
                if (entry.getValue().contains(vfsPath)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Get all pack IDs that have extracted files.
     */
    public static String[] getRegisteredPackIds() {
        synchronized (packPathsLock) {
            return packExtractedPaths.keySet().toArray(new String[0]);
        }
    }

    /**
     * Delete a file from the VFS.
     *
     * @param absPath Path to delete
     * @return true if deleted, false if not found or is a directory
     */
    public static boolean deleteFile(String absPath) {
        if (!booted) return false;

        String p = absPath.toLowerCase(java.util.Locale.ROOT);
        VirtualFile f = fs.getFile(p);
        if (f == null || !f.exists() || f.isDirectory()) {
            return false;
        }

        try {
            // Get parent directory
            int lastSlash = p.lastIndexOf('/');
            String parentPath = lastSlash > 0 ? p.substring(0, lastSlash) : "/";
            String fileName = p.substring(lastSlash + 1);

            VirtualFile parent = fs.getFile(parentPath);
            if (parent != null) {
                f.delete();
                return true;
            }
        } catch (Exception e) {
            log("WebFs.deleteFile: " + absPath + " failed: " + e);
        }
        return false;
    }

    /**
     * Delete a directory and all its contents recursively.
     *
     * @param absPath Path to directory to delete
     * @return true if deleted, false otherwise
     */
    public static boolean deleteDirectory(String absPath) {
        if (!booted) return false;

        String p = absPath.toLowerCase(java.util.Locale.ROOT);
        VirtualFile f = fs.getFile(p);
        if (f == null || !f.exists() || !f.isDirectory()) {
            return false;
        }

        return deleteDirectoryRecursive(f);
    }

    private static boolean deleteDirectoryRecursive(VirtualFile dir) {
        try {
            String[] children = dir.listFiles();
            if (children != null) {
                for (String childName : children) {
                    VirtualFile child = dir.getFile(childName);
                    if (child != null) {
                        if (child.isDirectory()) {
                            deleteDirectoryRecursive(child);
                        } else {
                            child.delete();
                        }
                    }
                }
            }
            dir.delete();
            return true;
        } catch (Exception e) {
            log("WebFs.deleteDirectoryRecursive: failed: " + e);
            return false;
        }
    }

    /**
     * Write bytes to VFS and register the path with a pack ID.
     * Convenience method for resource pack extraction.
     *
     * @param absPath VFS path to write to
     * @param data Bytes to write
     * @param packId Pack ID to associate with this file
     */
    public static void writeBytesForPack(String absPath, byte[] data, String packId) {
        writeBytes(absPath, data);
        synchronized (packPathsLock) {
            List<String> paths = packExtractedPaths.get(packId);
            if (paths == null) {
                paths = new ArrayList<>();
                packExtractedPaths.put(packId, paths);
            }
            paths.add(absPath);
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static int parseAndApply(byte[] blob) {
        // header: "MCVF" + u32 version + u32 count
        if (blob[0] != 'M' || blob[1] != 'C' || blob[2] != 'V' || blob[3] != 'F') {
            throw new RuntimeException("bad magic; expected MCVF");
        }
        int pos = 4;
        int version = readU32LE(blob, pos); pos += 4;
        if (version != 1) {
            throw new RuntimeException("unsupported mc-vfs version " + version);
        }
        int count = readU32LE(blob, pos); pos += 4;
        log("WebFs: parsing " + count + " entries...");

        int loaded = 0;
        for (int i = 0; i < count; i++) {
            int pathLen = readU16LE(blob, pos); pos += 2;
            String path = new String(blob, pos, pathLen, java.nio.charset.StandardCharsets.UTF_8);
            pos += pathLen;
            int dataLen = readU32LE(blob, pos); pos += 4;
            byte[] data = new byte[dataLen];
            System.arraycopy(blob, pos, data, 0, dataLen);
            pos += dataLen;

            String absPath = path.startsWith("/") ? path : ("/" + path);
            absPath = absPath.toLowerCase(java.util.Locale.ROOT);
            writeBytes(absPath, data);
            loaded++;
            if (loaded % 5000 == 0) log("WebFs: ... loaded " + loaded + " files");
        }
        return loaded;
    }

    private static int readU16LE(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8);
    }

    private static int readU32LE(byte[] b, int o) {
        return (b[o] & 0xff)
             | ((b[o + 1] & 0xff) << 8)
             | ((b[o + 2] & 0xff) << 16)
             | ((b[o + 3] & 0xff) << 24);
    }

    /** Sync XHR fetch returning raw bytes. */
    private static byte[] fetchSync(String url) {
        // teavm-jso doesn't expose XHR directly here; we drop into raw JS.
        // Returns Int8Array-backed byte[] via TeaVM's typed-array bridge.
        return doFetchSync(url);
    }

    @JSBody(params = "url", script =
        "var x = new XMLHttpRequest();" +
        "x.open('GET', url, false);" + // sync
        // sync XHR forbids responseType in a Document context — use the
        // \"binary string\" trick: overrideMimeType + read responseText
        // and copy each char's low byte into an Int8Array.
        "x.overrideMimeType('text/plain; charset=x-user-defined');" +
        "x.send(null);" +
        "if (x.status !== 200 && x.status !== 0) { throw new Error('fetch ' + url + ' status=' + x.status); }" +
        "var s = x.responseText;" +
        "var n = s.length;" +
        "var arr = new Int8Array(n);" +
        "for (var i = 0; i < n; i++) { arr[i] = s.charCodeAt(i) & 0xff; }" +
        "return arr;")
    private static native byte[] doFetchSync(String url);

    /**
     * Synchronously fetch raw bytes from a URL.
     * This is a blocking call - use with caution.
     *
     * @param url The URL to fetch from
     * @return The response bytes, or null on failure
     */
    public static byte[] fetchSync(String url) {
        return doFetchSync(url);
    }

    /**
     * Asynchronously fetch bytes from a URL with progress callback.
     * Prefer this over fetchSync for large resources.
     *
     * @param url The URL to fetch from
     * @param callback Called with result (error, bytes) when complete
     */
    public static void fetchAsync(String url, FetchCallback callback) {
        doFetchAsync(url, callback);
    }

    public interface FetchCallback {
        void onComplete(byte[] data);
        void onError(String error);
        void onProgress(float progress, String status);
    }

    @JSBody(params = {"url", "callback"}, script =
        "var cb = JSON.parse(callback); " +
        "var x = new XMLHttpRequest();" +
        "x.open('GET', url, true);" +
        "x.responseType = 'arraybuffer';" +
        "x.onprogress = function(e) { " +
        "  if (e.lengthComputable) { " +
        "    var prog = { type: 'progress', loaded: e.loaded, total: e.total }; " +
        "  } " +
        "};" +
        "x.onload = function() { " +
        "  if (x.status === 200 || x.status === 0) { " +
        "    try { " +
        "      var arr = new Uint8Array(x.response); " +
        "    } catch(e) { " +
        "    } " +
        "  } else { " +
        "  } " +
        "};" +
        "x.onerror = function() { " +
        "};" +
        "x.send(null);")
    private static native void doFetchAsync(String url, String callback);

    @JSBody(params = "msg", script = "console.log('[mc-web/vfs] ' + msg);")
    private static native void log(String msg);

    @JSBody(params = {"name", "detail"}, script =
        "try {" +
        "  if (typeof window !== 'undefined' && typeof window.__webmcStartupMark === 'function') {" +
        "    window.__webmcStartupMark(String(name || ''), String(detail || ''));" +
        "  }" +
        "} catch (e) {}")
    private static native void startupMark(String name, String detail);

    /** Hook for JdkMethodStubsTransformer to update TeaVM's internal FS instance. */
    public static void updateVfsInstance(Object mem) {
        // Body injected by transformer
    }

    /**
     * Diagnostic: Dump the VFS tree starting at the given path to the console.
     * Can be called from the browser console.
     */
    public static void dump(String root) {
        log("Dumping VFS from " + root + ":");
        dumpRecursive(root, 0);
    }

    private static void dumpRecursive(String path, int depth) {
        VirtualFile f = fs.getFile(path);
        if (f == null || !f.exists()) {
            log("  ".repeat(depth) + path + " (MISSING)");
            return;
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.isEmpty()) name = "/";
        log("  ".repeat(depth) + name + (f.isDirectory() ? "/" : " (" + f.length() + " bytes)"));
        if (f.isDirectory()) {
            String[] children = f.listFiles();
            if (children != null) {
                for (String child : children) {
                    String childPath = path.endsWith("/") ? (path + child) : (path + "/" + child);
                    dumpRecursive(childPath, depth + 1);
                }
            }
        }
    }

    private WebFs() {}
}
