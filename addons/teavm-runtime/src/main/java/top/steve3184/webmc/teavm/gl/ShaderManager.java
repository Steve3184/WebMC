package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSObject;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance shader manager with compilation caching.
 * Compiles and caches GLSL shaders for efficient rendering.
 */
public final class ShaderManager {

    private static WebGLRenderingContext gl;
    private static Map<String, WebGLProgram> programCache = new HashMap<>();
    private static Map<Integer, WebGLUniformLocation> uniformCache = new HashMap<>();
    private static int currentProgram = 0;
    private static boolean initialized = false;

    // Shader source templates (WebGL1 compatible - no #version 300 es)
    private static final String VERTEX_SHADER_3D =
        "attribute vec3 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "attribute vec3 aNormal;\n" +
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uModelView;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec3 vNormal;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vNormal = mat3(uModelView) * aNormal;\n" +
        "    vPosition = (uModelView * vec4(aPosition, 1.0)).xyz;\n" +
        "    gl_Position = uProjection * uModelView * vec4(aPosition, 1.0);\n" +
        "}";

    private static final String FRAGMENT_SHADER_3D =
        "precision mediump float;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec3 vNormal;\n" +
        "varying vec3 vPosition;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform vec3 uLightDir;\n" +
        "uniform vec3 uAmbient;\n" +
        "uniform float uFogStart;\n" +
        "uniform float uFogEnd;\n" +
        "uniform vec3 uFogColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
        "    float diff = max(dot(normalize(vNormal), uLightDir), 0.0);\n" +
        "    vec3 lighting = uAmbient + diff * vec3(1.0);\n" +
        "    vec3 color = texColor.rgb * lighting;\n" +
        "    float fogFactor = clamp((length(vPosition) - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);\n" +
        "    color = mix(color, uFogColor, fogFactor);\n" +
        "    gl_FragColor = vec4(color, texColor.a);\n" +
        "}";

    private static final String VERTEX_SHADER_2D =
        "attribute vec2 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "attribute vec4 aColor;\n" +
        "uniform mat4 uProjection;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vColor = aColor;\n" +
        "    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);\n" +
        "}";

    private static final String FRAGMENT_SHADER_2D =
        "precision mediump float;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform bool uHasTexture;\n" +
        "void main() {\n" +
        "    if (uHasTexture) {\n" +
        "        gl_FragColor = texture2D(uTexture, vTexCoord) * vColor;\n" +
        "    } else {\n" +
        "        gl_FragColor = vColor;\n" +
        "    }\n" +
        "}";

    private static final String VERTEX_SHADER_SKY =
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uModelView;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    vPosition = aPosition;\n" +
        "    mat4 rotMatrix = mat4(mat3(uModelView));\n" +
        "    gl_Position = uProjection * rotMatrix * vec4(aPosition, 1.0);\n" +
        "    gl_Position.z = gl_Position.w;\n" +
        "}";

    private static final String FRAGMENT_SHADER_SKY =
        "precision mediump float;\n" +
        "varying vec3 vPosition;\n" +
        "uniform vec3 uSkyColor;\n" +
        "uniform vec3 uHorizonColor;\n" +
        "uniform float uTime;\n" +
        "void main() {\n" +
        "    float y = normalize(vPosition).y;\n" +
        "    vec3 skyColor = mix(uHorizonColor, uSkyColor, max(y, 0.0));\n" +
        "    // Stars\n" +
        "    vec3 stars = vec3(0.0);\n" +
        "    if (y > 0.0) {\n" +
        "        float starNoise = fract(sin(dot(vPosition.xz, vec2(12.9898, 78.233))) * 43758.5453);\n" +
        "        if (starNoise > 0.997) stars = vec3(1.0) * (starNoise - 0.997) * 333.0;\n" +
        "    }\n" +
        "    gl_FragColor = vec4(skyColor + stars, 1.0);\n" +
        "}";

    private ShaderManager() {}

    /**
     * Initialize shader manager.
     */
    public static void init(WebGLRenderingContext glContext) {
        if (initialized) return;
        gl = glContext;
        programCache.clear();
        uniformCache.clear();
        initialized = true;
        log("[ShaderManager] Initialized");
    }

    /**
     * Get or create a shader program.
     */
    public static WebGLProgram getProgram(String name) {
        if (!initialized || gl == null) return null;
        if (programCache.containsKey(name)) {
            return programCache.get(name);
        }

        WebGLProgram program = null;
        switch (name) {
            case "basic3d":
                program = createProgram(VERTEX_SHADER_3D, FRAGMENT_SHADER_3D);
                break;
            case "basic2d":
                program = createProgram(VERTEX_SHADER_2D, FRAGMENT_SHADER_2D);
                break;
            case "sky":
                program = createProgram(VERTEX_SHADER_SKY, FRAGMENT_SHADER_SKY);
                break;
            default:
                log("[ShaderManager] Unknown program: " + name);
                return null;
        }

        if (program != null) {
            programCache.put(name, program);
            log("[ShaderManager] Created program: " + name);
        }
        return program;
    }

    /**
     * Create a shader program from source.
     */
    private static WebGLProgram createProgram(String vertexSrc, String fragmentSrc) {
        WebGLShader vertexShader = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSrc);
        WebGLShader fragmentShader = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSrc);

        if (vertexShader == null || fragmentShader == null) {
            return null;
        }

        WebGLProgram program = gl.createProgram();
        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);

        if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
            String info = gl.getProgramInfoLog(program);
            log("[ShaderManager] Link error: " + info);
            gl.deleteProgram(program);
            return null;
        }

        // Clean up shaders after linking
        gl.deleteShader(vertexShader);
        gl.deleteShader(fragmentShader);

        return program;
    }

    /**
     * Compile a shader.
     */
    private static WebGLShader compileShader(int type, String source) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);

        if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
            String info = gl.getShaderInfoLog(shader);
            log("[ShaderManager] Compile error (" + (type == WebGLRenderingContext.VERTEX_SHADER ? "VS" : "FS") + "): " + info);
            gl.deleteShader(shader);
            return null;
        }

        return shader;
    }

    /**
     * Use a shader program.
     */
    public static void useProgram(WebGLProgram program) {
        if (!initialized || gl == null) return;
        int programId = program != null ? program.hashCode() : 0;
        if (currentProgram != programId) {
            gl.useProgram(program);
            currentProgram = programId;
        }
    }

    /**
     * Get uniform location with caching.
     */
    public static WebGLUniformLocation getUniformLocation(WebGLProgram program, String name) {
        int cacheKey = (program != null ? program.hashCode() : 0) * 31 + name.hashCode();
        if (uniformCache.containsKey(cacheKey)) {
            return uniformCache.get(cacheKey);
        }

        WebGLUniformLocation location = gl.getUniformLocation(program, name);
        uniformCache.put(cacheKey, location);
        return location;
    }

    /**
     * Set uniform matrix 4x4.
     */
    public static void setUniformMatrix4(WebGLProgram program, String name, float[] matrix) {
        WebGLUniformLocation loc = getUniformLocation(program, name);
        if (loc != null) {
            gl.uniformMatrix4fv(loc, false, matrix);
        }
    }

    /**
     * Set uniform vector3.
     */
    public static void setUniform3f(WebGLProgram program, String name, float x, float y, float z) {
        WebGLUniformLocation loc = getUniformLocation(program, name);
        if (loc != null) {
            gl.uniform3f(loc, x, y, z);
        }
    }

    /**
     * Set uniform float.
     */
    public static void setUniform1f(WebGLProgram program, String name, float x) {
        WebGLUniformLocation loc = getUniformLocation(program, name);
        if (loc != null) {
            gl.uniform1f(loc, x);
        }
    }

    /**
     * Set uniform int.
     */
    public static void setUniform1i(WebGLProgram program, String name, int x) {
        WebGLUniformLocation loc = getUniformLocation(program, name);
        if (loc != null) {
            gl.uniform1i(loc, x);
        }
    }

    /**
     * Set uniform boolean.
     */
    public static void setUniformBool(WebGLProgram program, String name, boolean x) {
        WebGLUniformLocation loc = getUniformLocation(program, name);
        if (loc != null) {
            gl.uniform1i(loc, x ? 1 : 0);
        }
    }

    /**
     * Bind attribute locations.
     */
    public static void bindAttribLocations(WebGLProgram program, int position, String name) {
        gl.bindAttribLocation(program, position, name);
    }

    /**
     * Get current program ID.
     */
    public static int getCurrentProgram() {
        return currentProgram;
    }

    /**
     * Check if initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Delete all cached programs.
     */
    public static void destroy() {
        if (!initialized || gl == null) return;
        for (WebGLProgram program : programCache.values()) {
            gl.deleteProgram(program);
        }
        programCache.clear();
        uniformCache.clear();
        currentProgram = 0;
        log("[ShaderManager] Destroyed all programs");
    }

    private static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
