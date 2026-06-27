package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import top.steve3184.webmc.web.WebDiagnostics;

/**
 * Strategy-B backend: implements MC 1.21.8's {@link GpuDevice} SPI on top of
 * WebGL 2.0. Replaces {@code com.mojang.blaze3d.opengl.GlDevice}; the only
 * patch needed is in {@code RenderSystem.java} where {@code new GlDevice(...)}
 * becomes {@code new WebGpuDevice(...)}.
 *
 * Constructor signature MIRRORS GlDevice exactly so the patch is one-line.
 *
 * Phase 1: createXxx return tracking-only stub objects (no GL allocation).
 * Phase 2 will wire actual WebGL2 calls through WebGLBackend.
 */
public class WebGpuDevice implements GpuDevice {

    private final long windowHandle;
    private final int debug;
    private final BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource;
    private final CommandEncoder encoder;
    private final int maxTextureSize;
    private final Map<RenderPipeline, WebCompiledRenderPipeline> pipelineCache = new ConcurrentHashMap<>();
    private final Map<ShaderTranslationKey, String> translatedShaderSourceCache = new ConcurrentHashMap<>();
    private static final String TRANSLATION_REFUSED = "\u0000WEBMC_TRANSLATION_REFUSED";
    private static long webLazyCompileTimelineStartMs;

    public WebGpuDevice(long p_391790_,
                        int p_397807_,
                        boolean p_394690_,
                        BiFunction<ResourceLocation, ShaderType, String> p_392078_,
                        boolean p_396865_) {
        this.windowHandle = p_391790_;
        this.debug = p_397807_;
        this.defaultShaderSource = p_392078_;
        this.encoder = new WebCommandEncoder(this);
        // WebGL2 minimum guaranteed: 2048; query at runtime via gl.getParameter(MAX_TEXTURE_SIZE).
        this.maxTextureSize = 8192;
    }

    @Override public CommandEncoder createCommandEncoder() { return this.encoder; }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, int usage, TextureFormat fmt,
                                    int width, int height, int depthOrLayers, int mipLevels) {
        return new WebGpuTexture(usage, label != null ? label.get() : null, fmt, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, int usage, TextureFormat fmt,
                                    int width, int height, int depthOrLayers, int mipLevels) {
        return new WebGpuTexture(usage, label, fmt, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture tex) {
        return new WebGpuTextureView(tex, 0, tex.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture tex, int baseMip, int mipLevels) {
        return new WebGpuTextureView(tex, baseMip, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, int size) {
        return new WebGpuBuffer(label != null ? label.get() : null, usage, size);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, ByteBuffer initialData) {
        WebGpuBuffer buf = new WebGpuBuffer(label != null ? label.get() : null, usage, initialData.remaining());
        buf.uploadAt(0, initialData);
        return buf;
    }

    @Override public String getImplementationInformation() { return "WebGL2 (mc-web phase 1 stub)"; }

    private org.teavm.jso.webgl.WebGLBuffer uboPlaceholderBuf;
    /** Shared 4KiB UBO bound to any declared-but-unset slot during setPipeline. */
    public org.teavm.jso.webgl.WebGLBuffer uboPlaceholder() {
        if (this.uboPlaceholderBuf != null) return this.uboPlaceholderBuf;
        if (!top.steve3184.webmc.teavm.gl.WebGLContextHolder.isInstalled()) return null;
        org.teavm.jso.webgl.WebGL2RenderingContext gl = top.steve3184.webmc.teavm.gl.WebGLContextHolder.gl();
        org.teavm.jso.webgl.WebGLBuffer b = gl.createBuffer();
        gl.bindBuffer(org.teavm.jso.webgl.WebGL2RenderingContext.UNIFORM_BUFFER, b);
        gl.bufferData(org.teavm.jso.webgl.WebGL2RenderingContext.UNIFORM_BUFFER, 4096, org.teavm.jso.webgl.WebGL2RenderingContext.STATIC_DRAW);
        this.uboPlaceholderBuf = b;
        return b;
    }

    @Override public List<String> getLastDebugMessages() { return Collections.emptyList(); }
    @Override public boolean isDebuggingEnabled() { return debug != 0; }
    @Override public String getVendor() { return "WebGL2"; }
    @Override public String getBackendName() { return "Web"; }
    @Override public String getVersion() { return "WebGL2/3.0"; }
    @Override public String getRenderer() { return "Browser WebGL2"; }
    @Override public int getMaxTextureSize() { return maxTextureSize; }

    @Override public int getUniformOffsetAlignment() {
        // WebGL2 minimum guaranteed: 256. Should query GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT.
        return 256;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline,
                                                     @Nullable BiFunction<ResourceLocation, ShaderType, String> shaderSourceLoader) {
        WebCompiledRenderPipeline cached = pipelineCache.get(pipeline);
        if (cached != null) return cached;
        boolean lazyCompile = shaderSourceLoader == null && WebDiagnostics.enabled();
        long startMs = lazyCompile ? System.currentTimeMillis() : 0L;
        BiFunction<ResourceLocation, ShaderType, String> loader =
            shaderSourceLoader != null ? shaderSourceLoader : this.defaultShaderSource;
        WebCompiledRenderPipeline built = buildPipeline(pipeline, loader);
        pipelineCache.put(pipeline, built);
        if (lazyCompile) {
            if (webLazyCompileTimelineStartMs == 0L) {
                webLazyCompileTimelineStartMs = startMs;
            }
            int durationMs = (int)Math.min((long)Integer.MAX_VALUE, Math.max(0L, System.currentTimeMillis() - startMs));
            WebDiagnostics.timelineEvent(
                "shaderLazyCompileEvents",
                "compile",
                pipeline.getLocation() + " durationMs=" + durationMs + " valid=" + built.isValid(),
                durationMs,
                webLazyCompileTimelineStartMs
            );
        }
        return built;
    }

    /** Public access for WebRenderPass.setPipeline to look up a cached compile. */
    public WebCompiledRenderPipeline lookupOrCompile(RenderPipeline pipeline) {
        return (WebCompiledRenderPipeline) precompilePipeline(pipeline, null);
    }

    private WebCompiledRenderPipeline buildPipeline(RenderPipeline pipeline,
                                                     BiFunction<ResourceLocation, ShaderType, String> loader) {
        boolean diagnostics = WebDiagnostics.enabled();
        long buildStartMs = diagnostics ? System.currentTimeMillis() : 0L;
        long stageStartMs = buildStartMs;
        String pipelineId = diagnostics ? pipeline.getLocation().toString() : "";
        if (loader == null) {
            System.err.println("[mc-web/gl] no shader source loader for " + pipeline.getLocation());
            return new WebCompiledRenderPipeline(pipeline, "no shader source loader");
        }
        String vsh = loader.apply(pipeline.getVertexShader(), ShaderType.VERTEX);
        String fsh = loader.apply(pipeline.getFragmentShader(), ShaderType.FRAGMENT);
        if (vsh == null) {
            vsh = loadShaderFromWebFs(pipeline.getVertexShader(), ShaderType.VERTEX);
        }
        if (fsh == null) {
            fsh = loadShaderFromWebFs(pipeline.getFragmentShader(), ShaderType.FRAGMENT);
        }
        if (vsh == null || fsh == null) {
            System.err.println("[mc-web/gl] missing shader source for " + pipeline.getLocation()
                + " vsh=" + (vsh != null) + " fsh=" + (fsh != null));
            return new WebCompiledRenderPipeline(pipeline, "missing shader source");
        }
        if (diagnostics) {
            timelineStage("source", pipelineId + " vsh=" + pipeline.getVertexShader() + " fsh=" + pipeline.getFragmentShader(), stageStartMs, buildStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        vsh = translateCached(pipeline.getVertexShader(), ShaderType.VERTEX, vsh, diagnostics, pipelineId, buildStartMs);
        fsh = translateCached(pipeline.getFragmentShader(), ShaderType.FRAGMENT, fsh, diagnostics, pipelineId, buildStartMs);
        if (vsh == null || fsh == null) {
            System.err.println("[mc-web/gl] translator refused " + pipeline.getLocation() + " (unsupported GLSL construct, e.g. samplerBuffer)");
            return new WebCompiledRenderPipeline(pipeline, "translator refused");
        }
        if (diagnostics) {
            timelineStage("translate", pipelineId + " vshLen=" + vsh.length() + " fshLen=" + fsh.length(), stageStartMs, buildStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        String defines = pipeline.getShaderDefines().asSourceDirectives();
        vsh = injectTranslatedDefines(vsh, defines);
        fsh = injectTranslatedDefines(fsh, defines);
        if (diagnostics) {
            timelineStage("inject", pipelineId + " definesLen=" + (defines == null ? 0 : defines.length()) + " vshLen=" + vsh.length() + " fshLen=" + fsh.length(), stageStartMs, buildStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        WebCompiledRenderPipeline compiled = new WebCompiledRenderPipeline(
            top.steve3184.webmc.teavm.gl.WebGLContextHolder.gl(),
            pipeline, vsh, fsh, buildStartMs);
        if (diagnostics) {
            timelineStage("gl:total", pipelineId + " valid=" + compiled.isValid(), stageStartMs, buildStartMs);
        }
        return compiled;
    }

    @Nullable
    private String translateCached(ResourceLocation id,
                                   ShaderType type,
                                   String source,
                                   boolean diagnostics,
                                   String pipelineId,
                                   long buildStartMs) {
        ShaderTranslationKey key = new ShaderTranslationKey(id, type, source);
        String cached = translatedShaderSourceCache.get(key);
        if (cached != null) {
            if (diagnostics) {
                timelineTranslationCache("hit", pipelineId, id, type, 0, source.length(), cached, buildStartMs);
            }
            return TRANSLATION_REFUSED.equals(cached) ? null : cached;
        }

        long startMs = diagnostics ? System.currentTimeMillis() : 0L;
        String detail = diagnostics
            ? pipelineId + " shader=" + id + " type=" + type + " sourceLen=" + source.length()
            : null;
        String translated = top.steve3184.webmc.gpu.GlslTranslator.translate(source, type == ShaderType.FRAGMENT, detail, startMs);
        int durationMs = diagnostics
            ? (int)Math.min((long)Integer.MAX_VALUE, Math.max(0L, System.currentTimeMillis() - startMs))
            : 0;
        translatedShaderSourceCache.put(key, translated != null ? translated : TRANSLATION_REFUSED);
        if (diagnostics) {
            timelineTranslationCache(translated == null ? "miss:refused" : "miss", pipelineId, id, type, durationMs, source.length(), translated, buildStartMs);
        }
        return translated;
    }

    private void timelineTranslationCache(String phase,
                                          String pipelineId,
                                          ResourceLocation id,
                                          ShaderType type,
                                          int durationMs,
                                          int sourceLen,
                                          @Nullable String translated,
                                          long buildStartMs) {
        String detail = pipelineId
            + " shader=" + id
            + " type=" + type
            + " sourceLen=" + sourceLen
            + " translatedLen=" + (translated == null || TRANSLATION_REFUSED.equals(translated) ? -1 : translated.length())
            + " cacheSize=" + translatedShaderSourceCache.size()
            + " durationMs=" + durationMs;
        WebDiagnostics.timelineEvent("shaderTranslationCacheEvents", phase, detail, durationMs, buildStartMs);
    }

    private static String injectTranslatedDefines(String src, String defines) {
        if (defines == null || defines.isEmpty()) return src;
        int insertAt = src.indexOf("\n\n");
        if (insertAt >= 0 && src.startsWith("#version")) {
            return src.substring(0, insertAt + 2) + defines + src.substring(insertAt + 2);
        }
        return inject(src, defines);
    }

    private static final class ShaderTranslationKey {
        private final ResourceLocation id;
        private final ShaderType type;
        private final String source;
        private final int hash;

        ShaderTranslationKey(ResourceLocation id, ShaderType type, String source) {
            this.id = id;
            this.type = type;
            this.source = source;
            int h = 17;
            h = 31 * h + id.hashCode();
            h = 31 * h + type.hashCode();
            h = 31 * h + source.hashCode();
            this.hash = h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ShaderTranslationKey)) return false;
            ShaderTranslationKey other = (ShaderTranslationKey)obj;
            return this.type == other.type
                && this.id.equals(other.id)
                && this.source.equals(other.source);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    private static void timelineStage(String phase, String detail, long stageStartMs, long buildStartMs) {
        int durationMs = (int)Math.min((long)Integer.MAX_VALUE, Math.max(0L, System.currentTimeMillis() - stageStartMs));
        WebDiagnostics.timelineEvent("shaderPipelineStageEvents", phase, detail + " durationMs=" + durationMs, durationMs, buildStartMs);
    }

    @Nullable
    private static String loadShaderFromWebFs(ResourceLocation id, ShaderType type) {
        String path = "/assets/" + id.getNamespace() + "/shaders/" + id.getPath() + (type == ShaderType.VERTEX ? ".vsh" : ".fsh");
        byte[] bytes = top.steve3184.webmc.vfs.WebFs.readBytes(path);
        if (bytes == null) return null;
        return expandMojImports(new String(bytes, StandardCharsets.UTF_8), id.getNamespace(), new HashSet<>());
    }

    private static String expandMojImports(String source, String namespace, Set<String> seen) {
        StringBuilder out = new StringBuilder(source.length() + 256);
        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#moj_import")) {
                String importId = extractImportId(trimmed);
                String include = importId == null ? null : loadIncludeFromWebFs(importId, namespace, seen);
                if (include != null) {
                    out.append(include).append('\n');
                }
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    @Nullable
    private static String extractImportId(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>', start + 1);
        if (start >= 0 && end > start) return line.substring(start + 1, end);
        start = line.indexOf('"');
        end = line.indexOf('"', start + 1);
        if (start >= 0 && end > start) return line.substring(start + 1, end);
        return null;
    }

    @Nullable
    private static String loadIncludeFromWebFs(String importId, String defaultNamespace, Set<String> seen) {
        String namespace = defaultNamespace;
        String path = importId;
        int colon = importId.indexOf(':');
        if (colon >= 0) {
            namespace = importId.substring(0, colon);
            path = importId.substring(colon + 1);
        }
        String key = namespace + ":" + path;
        if (!seen.add(key)) return "";
        String absPath = "/assets/" + namespace + "/shaders/include/" + path;
        byte[] bytes = top.steve3184.webmc.vfs.WebFs.readBytes(absPath);
        if (bytes == null) return null;
        return expandMojImports(new String(bytes, StandardCharsets.UTF_8), namespace, seen);
    }

    /** Insert ShaderDefines right after the (first) #version line, if any; else prepend. */
    private static String inject(String src, String defines) {
        if (defines == null || defines.isEmpty()) return src;
        int nl = src.indexOf('\n');
        String first = nl > 0 ? src.substring(0, nl).trim() : "";
        if (first.startsWith("#") && first.contains("version")) {
            return src.substring(0, nl + 1) + defines + src.substring(nl + 1);
        }
        return defines + src;
    }

    @Override public void clearPipelineCache() {
        pipelineCache.clear();
        translatedShaderSourceCache.clear();
        webLazyCompileTimelineStartMs = 0L;
    }
    @Override public List<String> getEnabledExtensions() { return Collections.emptyList(); }
    @Override public void close() { /* phase 2: release tracked resources */ }
}
