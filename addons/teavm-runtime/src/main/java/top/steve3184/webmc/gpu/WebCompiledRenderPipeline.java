package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import top.steve3184.webmc.web.WebDiagnostics;

/**
 * Compiled program wrapper. Built once per RenderPipeline by
 * {@link WebGpuDevice#precompilePipeline}. Holds the GL program plus all
 * lookup tables we need at draw time:
 *
 *   attribLoc      element-name → vertex attribute location (or -1 if missing)
 *   samplerUnit    sampler-name → texture unit (assigned at link, persistent)
 *   samplerLoc     sampler-name → uniform location (so bindSampler knows what
 *                  to set if the program is rebound; also useful for debug)
 *   uboBinding     uniform-block-name → binding point (assigned at link)
 *
 * Render state (blend, depth, cull, write masks, polygon offset, primitive
 * mode) is cached as primitive fields so WebRenderPass.setPipeline can apply
 * the entire state vector with no further indirection.
 */
public final class WebCompiledRenderPipeline implements CompiledRenderPipeline {

    private final RenderPipeline pipeline;
    private final WebGLProgram program;
    private final boolean valid;
    private final String compileLog;

    private final Map<String, Integer> attribLoc = new HashMap<>();
    private final Map<String, Integer> samplerUnit = new HashMap<>();
    private final Map<String, WebGLUniformLocation> samplerLoc = new HashMap<>();
    private final Map<String, Integer> uboBinding = new HashMap<>();
    private final Map<String, Integer> uboBlockSize = new HashMap<>();

    private final VertexFormat vertexFormat;
    private final VertexFormat.Mode mode;

    /** Construct a failed pipeline — keeps MC alive but draws will be skipped. */
    public WebCompiledRenderPipeline(RenderPipeline pipeline, String reason) {
        this.pipeline = pipeline;
        this.program = null;
        // mc-web: report success even on failure, otherwise ShaderManager.apply
        // throws RuntimeException and leaves the shader cache empty (taking
        // post-chain configs like 'blur' with it). Draw-time checks on
        // program == null cause us to silently skip invalid pipelines, which
        // is the least-disruptive degradation path for the browser backend.
        this.valid = true;
        this.compileLog = reason;
        this.vertexFormat = pipeline.getVertexFormat();
        this.mode = pipeline.getVertexFormatMode();
    }

    public WebCompiledRenderPipeline(WebGL2RenderingContext gl,
                                     RenderPipeline pipeline,
                                     String vertexSource,
                                     String fragmentSource) {
        this(gl, pipeline, vertexSource, fragmentSource, 0L);
    }

    public WebCompiledRenderPipeline(WebGL2RenderingContext gl,
                                     RenderPipeline pipeline,
                                     String vertexSource,
                                     String fragmentSource,
                                     long diagnosticStartMs) {
        this.pipeline = pipeline;
        this.vertexFormat = pipeline.getVertexFormat();
        this.mode = pipeline.getVertexFormatMode();
        boolean diagnostics = diagnosticStartMs != 0L && WebDiagnostics.enabled();
        long stageStartMs = diagnostics ? System.currentTimeMillis() : 0L;
        String pipelineId = diagnostics ? pipeline.getLocation().toString() : "";

        WebGLShader vsh = compileShader(gl, WebGL2RenderingContext.VERTEX_SHADER, vertexSource, pipeline.getVertexShader().toString());
        if (diagnostics) {
            timelineStage("gl:vertex", pipelineId + " shader=" + pipeline.getVertexShader() + " ok=" + (vsh != null), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        WebGLShader fsh = compileShader(gl, WebGL2RenderingContext.FRAGMENT_SHADER, fragmentSource, pipeline.getFragmentShader().toString());
        if (diagnostics) {
            timelineStage("gl:fragment", pipelineId + " shader=" + pipeline.getFragmentShader() + " ok=" + (fsh != null), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        if (vsh == null || fsh == null) {
            this.program = null;
            this.valid = true; // see note in failure ctor
            this.compileLog = "shader compile failed";
            if (vsh != null) gl.deleteShader(vsh);
            if (fsh != null) gl.deleteShader(fsh);
            if (diagnostics) {
                timelineStage("gl:failed", pipelineId + " compile failed", stageStartMs, diagnosticStartMs);
            }
            return;
        }

        WebGLProgram prog = gl.createProgram();
        gl.attachShader(prog, vsh);
        gl.attachShader(prog, fsh);
        gl.linkProgram(prog);
        gl.deleteShader(vsh);
        gl.deleteShader(fsh);

        Object linkStatus = getProgramParameter(gl, prog, WebGL2RenderingContext.LINK_STATUS);
        if (diagnostics) {
            timelineStage("gl:link", pipelineId + " ok=" + truthy(linkStatus), stageStartMs, diagnosticStartMs);
            stageStartMs = System.currentTimeMillis();
        }
        if (!truthy(linkStatus)) {
            String log = gl.getProgramInfoLog(prog);
            System.err.println("[mc-web/gl] link FAILED " + pipeline.getLocation() + ": " + log);
            gl.deleteProgram(prog);
            this.program = null;
            this.valid = true; // see note in failure ctor
            this.compileLog = log;
            if (diagnostics) {
                timelineStage("gl:failed", pipelineId + " link failed", stageStartMs, diagnosticStartMs);
            }
            return;
        }

        this.program = prog;
        this.valid = true;
        this.compileLog = null;

        // Cache attribute locations (one per VertexFormat element).
        for (VertexFormatElement el : vertexFormat.getElements()) {
            String name = vertexFormat.getElementName(el);
            int loc = gl.getAttribLocation(prog, name);
            attribLoc.put(name, loc);
        }

        // Assign texture units for each sampler and persist them via uniform1i.
        // After this, switching textures only requires activeTexture+bindTexture.
        List<String> samplers = pipeline.getSamplers();
        gl.useProgram(prog);
        for (int i = 0; i < samplers.size(); i++) {
            String name = samplers.get(i);
            WebGLUniformLocation loc = gl.getUniformLocation(prog, name);
            samplerLoc.put(name, loc);
            samplerUnit.put(name, i);
            if (loc != null) {
                gl.uniform1i(loc, i);
            }
        }
        gl.useProgram(null);

        // Assign UBO binding points; bind index in program once, runtime just
        // bindBufferBase(UNIFORM_BUFFER, bindingPoint, bufferHandle).
        List<RenderPipeline.UniformDescription> uniforms = pipeline.getUniforms();
        int nextBinding = 0;
        for (RenderPipeline.UniformDescription u : uniforms) {
            if (u.type() != UniformType.UNIFORM_BUFFER) continue;
            int blockIndex = gl.getUniformBlockIndex(prog, u.name());
            if (blockIndex == WebGL2RenderingContext.INVALID_INDEX) {
                // uniform might be optimized out — skip silently
                continue;
            }
            int binding = nextBinding++;
            gl.uniformBlockBinding(prog, blockIndex, binding);
            uboBinding.put(u.name(), binding);
            int dataSize = getActiveUniformBlockDataSize(gl, prog, blockIndex);
            uboBlockSize.put(u.name(), dataSize);
        }
        if (diagnostics) {
            timelineStage(
                "gl:reflect",
                pipelineId + " attribs=" + vertexFormat.getElements().size() + " samplers=" + samplers.size() + " ubos=" + uboBinding.size(),
                stageStartMs,
                diagnosticStartMs
            );
        }
    }

    private static void timelineStage(String phase, String detail, long stageStartMs, long diagnosticStartMs) {
        int durationMs = (int)Math.min((long)Integer.MAX_VALUE, Math.max(0L, System.currentTimeMillis() - stageStartMs));
        WebDiagnostics.timelineEvent("shaderPipelineStageEvents", phase, detail + " durationMs=" + durationMs, durationMs, diagnosticStartMs);
    }

    @org.teavm.jso.JSBody(params = {"gl", "prog", "idx"}, script =
        "return gl.getActiveUniformBlockParameter(prog, idx, gl.UNIFORM_BLOCK_DATA_SIZE);")
    private static native int getActiveUniformBlockDataSize(WebGL2RenderingContext gl, WebGLProgram prog, int blockIndex);

    private static WebGLShader compileShader(WebGL2RenderingContext gl, int type, String src, String name) {
        WebGLShader sh = gl.createShader(type);
        gl.shaderSource(sh, src);
        gl.compileShader(sh);
        Object status = getShaderParameter(gl, sh, WebGL2RenderingContext.COMPILE_STATUS);
        if (!truthy(status)) {
            String log = gl.getShaderInfoLog(sh);
            String typeName = (type == WebGL2RenderingContext.VERTEX_SHADER) ? "vsh" : "fsh";
            System.err.println("[mc-web/gl] " + typeName + " compile FAILED " + name + ": " + log);
            // dump first lines of source for context
            String[] lines = src.split("\n");
            int dump = Math.min(lines.length, 25);
            for (int i = 0; i < dump; i++) {
                System.err.println("  src[" + (i + 1) + "] " + lines[i]);
            }
            gl.deleteShader(sh);
            return null;
        }
        return sh;
    }

    @org.teavm.jso.JSBody(params = {"gl", "sh", "p"}, script = "return gl.getShaderParameter(sh, p);")
    private static native Object getShaderParameter(WebGL2RenderingContext gl, WebGLShader sh, int p);

    @org.teavm.jso.JSBody(params = {"gl", "prog", "p"}, script = "return gl.getProgramParameter(prog, p);")
    private static native Object getProgramParameter(WebGL2RenderingContext gl, WebGLProgram prog, int p);

    @org.teavm.jso.JSBody(params = "v", script = "return v ? 1 : 0;")
    private static native int truthyJs(Object v);

    private static boolean truthy(Object v) { return truthyJs(v) != 0; }

    @Override public boolean isValid() { return valid; }

    public RenderPipeline pipeline()   { return pipeline; }
    public WebGLProgram program()      { return program; }
    public VertexFormat vertexFormat() { return vertexFormat; }
    public VertexFormat.Mode mode()    { return mode; }
    public Integer attribLocation(String name)        { return attribLoc.get(name); }
    public Integer samplerUnitFor(String name)        { return samplerUnit.get(name); }
    public Integer uboBindingFor(String name)         { return uboBinding.get(name); }
    public Integer uboBlockSizeFor(String name)       { return uboBlockSize.get(name); }
    public int[] allUboBindings() {
        java.util.Collection<Integer> values = uboBinding.values();
        int[] result = new int[values.size()];
        int i = 0;
        for (Integer v : values) {
            result[i++] = v;
        }
        return result;
    }
    public String debugUboNames() { return uboBinding.keySet().toString(); }
    public String compileLog()                         { return compileLog; }
}
