package top.steve3184.webmc.teavm.gl;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSString;
import org.teavm.jso.webgl.*;
import top.steve3184.webmc.teavm.WebLog;

/**
 * Shader manager with Minecraft-compatible shaders.
 * Provides optimized shaders for terrain, entities, UI, and particles.
 */
public final class ShaderManager {

    private static WebGLRenderingContext gl;
    private static WebGLProgram currentProgram;
    private static String currentShaderName = "";

    private static WebGLProgram terrainProgram;
    private static WebGLProgram terrainColoredProgram;
    private static WebGLProgram entityProgram;
    private static WebGLProgram entityCutoutProgram;
    private static WebGLProgram particlesProgram;
    private static WebGLProgram positionTexProgram;
    private static WebGLProgram positionTexColProgram;
    private static WebGLProgram positionColorProgram;
    private static WebGLProgram positionColorLightmapProgram;
    private static WebGLProgram positionTexLightmapProgram;
    private static WebGLProgram skyProgram;
    private static WebGLProgram cloudsProgram;

    // Attribute locations
    private static int attrPosition = -1;
    private static int attrTexCoord = -1;
    private static int attrNormal = -1;
    private static int attrColor = -1;
    private static int attrLightMap = -1;
    private static int attrOverlay = -1;
    private static int attrNormal2 = -1;
    private static int attrMidBlock = -1;

    private static boolean initialized = false;

    private ShaderManager() {}

    /**
     * Initialize all shaders.
     */
    public static void init(WebGLRenderingContext webgl) {
        if (initialized) return;
        gl = webgl;

        WebLog.info("[ShaderManager] Initializing shaders...");

        terrainProgram = createProgram("terrain", TERRAIN_VERTEX, TERRAIN_FRAGMENT);
        terrainColoredProgram = createProgram("terrain_colored", TERRAIN_COLORED_VERTEX, TERRAIN_FRAGMENT);
        entityProgram = createProgram("entity", ENTITY_VERTEX, ENTITY_FRAGMENT);
        entityCutoutProgram = createProgram("entity_cutout", ENTITY_CUTOUT_VERTEX, ENTITY_CUTOUT_FRAGMENT);
        particlesProgram = createProgram("particles", PARTICLES_VERTEX, PARTICLES_FRAGMENT);
        positionTexProgram = createProgram("position_tex", POSITION_TEX_VERTEX, POSITION_TEX_FRAGMENT);
        positionTexColProgram = createProgram("position_tex_col", POSITION_TEX_COL_VERTEX, POSITION_TEX_COL_FRAGMENT);
        positionColorProgram = createProgram("position_color", POSITION_COLOR_VERTEX, POSITION_COLOR_FRAGMENT);
        positionColorLightmapProgram = createProgram("position_color_lm", POSITION_COLOR_LIGHTMAP_VERTEX, POSITION_COLOR_LIGHTMAP_FRAGMENT);
        positionTexLightmapProgram = createProgram("position_tex_lm", POSITION_TEX_LIGHTMAP_VERTEX, POSITION_TEX_LIGHTMAP_FRAGMENT);
        skyProgram = createProgram("sky", SKY_VERTEX, SKY_FRAGMENT);
        cloudsProgram = createProgram("clouds", CLOUDS_VERTEX, CLOUDS_FRAGMENT);

        // Cache attribute locations for terrain program
        if (terrainProgram != null) {
            attrPosition = gl.getAttribLocation(terrainProgram, "Position");
            attrTexCoord = gl.getAttribLocation(terrainProgram, "TexCoord");
            attrNormal = gl.getAttribLocation(terrainProgram, "Normal");
            attrColor = gl.getAttribLocation(terrainProgram, "Color");
            attrLightMap = gl.getAttribLocation(terrainProgram, "LightMap");
            WebLog.info("[ShaderManager] Terrain shader attribute locations: pos=" + attrPosition +
                       " tex=" + attrTexCoord + " norm=" + attrNormal + " col=" + attrColor + " lm=" + attrLightMap);
        }

        initialized = true;
        WebLog.info("[ShaderManager] All shaders initialized");
    }

    /**
     * Use terrain shader (blocks, terrain).
     */
    public static void useTerrainShader() {
        useProgram(terrainProgram, "terrain");
    }

    /**
     * Use entity shader (mobs, items).
     */
    public static void useEntityShader() {
        useProgram(entityProgram, "entity");
    }

    /**
     * Use entity cutout shader (transparent entities).
     */
    public static void useEntityCutoutShader() {
        useProgram(entityCutoutProgram, "entity_cutout");
    }

    /**
     * Use particles shader.
     */
    public static void useParticlesShader() {
        useProgram(particlesProgram, "particles");
    }

    /**
     * Use position + texture shader.
     */
    public static void usePositionTexShader() {
        useProgram(positionTexProgram, "position_tex");
    }

    /**
     * Use sky shader.
     */
    public static void useSkyShader() {
        useProgram(skyProgram, "sky");
    }

    /**
     * Use clouds shader.
     */
    public static void useCloudsShader() {
        useProgram(cloudsProgram, "clouds");
    }

    private static void useProgram(WebGLProgram program, String name) {
        if (program == null) return;
        if (currentProgram != program) {
            gl.useProgram(program);
            currentProgram = program;
            currentShaderName = name;
        }
    }

    public static String getCurrentShader() {
        return currentShaderName;
    }

    private static WebGLProgram createProgram(String name, String vertexSrc, String fragmentSrc) {
        WebGLShader vs = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSrc);
        WebGLShader fs = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSrc);

        if (vs == null || fs == null) {
            WebLog.error("[ShaderManager] Failed to compile " + name + " shader");
            return null;
        }

        WebGLProgram program = gl.createProgram();
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);

        if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
            String info = gl.getProgramInfoLog(program);
            WebLog.error("[ShaderManager] Link error for " + name + ": " + info);
            return null;
        }

        WebLog.info("[ShaderManager] Compiled shader: " + name);
        return program;
    }

    private static WebGLShader compileShader(int type, String source) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);

        if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
            String info = gl.getShaderInfoLog(shader);
            WebLog.error("[ShaderManager] Compile error: " + info);
            gl.deleteShader(shader);
            return null;
        }
        return shader;
    }

    // ==================== SHADER SOURCES ====================

    // Terrain vertex shader - matches Minecraft's format
    private static final String TERRAIN_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute vec3 Normal;\n" +
        "attribute vec4 Color;\n" +
        "attribute vec2 LightMap;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying float FogDist;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Color0 = Color;\n" +
        "  Normal0 = Normal;\n" +
        "  LightMap0 = LightMap;\n" +
        "  FogDist = length(gl_Position.xyz);\n" +
        "}\n";

    private static final String TERRAIN_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying float FogDist;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform sampler2D LightMap;\n" +
        "uniform vec3 FogColor;\n" +
        "uniform float FogStart;\n" +
        "uniform float FogEnd;\n" +
        "uniform float GameTime;\n" +
        "uniform float BlurFactor;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  if (tex.a < 0.1) discard;\n" +
        "  vec4 light = texture2D(LightMap, LightMap0 / 240.0);\n" +
        "  gl_FragColor = tex * Color0 * light;\n" +
        "  float fogFactor = clamp((FogDist - FogStart) / (FogEnd - FogStart), 0.0, 1.0);\n" +
        "  gl_FragColor.rgb = mix(gl_FragColor.rgb, FogColor, fogFactor);\n" +
        "}\n";

    // Terrain colored vertex shader
    private static final String TERRAIN_COLORED_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute vec3 Normal;\n" +
        "attribute vec4 Color;\n" +
        "attribute vec2 LightMap;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying float FogDist;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Color0 = Color;\n" +
        "  Normal0 = Normal;\n" +
        "  LightMap0 = LightMap;\n" +
        "  FogDist = length(gl_Position.xyz);\n" +
        "}\n";

    // Entity vertex shader - with diffuse lighting
    private static final String ENTITY_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute vec3 Normal;\n" +
        "attribute vec4 Color;\n" +
        "attribute vec2 LightMap;\n" +
        "attribute vec2 Overlay;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "uniform vec3 ColorModulator;\n" +
        "uniform float GameTime;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying vec2 Overlay0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Color0 = Color * vec4(ColorModulator, 1.0);\n" +
        "  Normal0 = Normal;\n" +
        "  LightMap0 = LightMap;\n" +
        "  Overlay0 = Overlay;\n" +
        "}\n";

    private static final String ENTITY_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying vec2 Overlay0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform sampler2D LightMap;\n" +
        "uniform vec3 OverrideColor;\n" +
        "uniform float Shade;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  if (tex.a < 0.1) discard;\n" +
        "  vec4 light = texture2D(LightMap, LightMap0 / 240.0);\n" +
        "  vec3 normal = normalize(Normal0);\n" +
        "  vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));\n" +
        "  float diffuse = max(dot(normal, lightDir), 0.0);\n" +
        "  gl_FragColor = tex * Color0 * light * (0.5 + 0.5 * diffuse * Shade);\n" +
        "}\n";

    // Entity cutout shader (with alpha cutout)
    private static final String ENTITY_CUTOUT_VERTEX = ENTITY_VERTEX;

    private static final String ENTITY_CUTOUT_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "varying vec3 Normal0;\n" +
        "varying vec2 LightMap0;\n" +
        "varying vec2 Overlay0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform sampler2D LightMap;\n" +
        "uniform vec3 ColorModulator;\n" +
        "uniform float Cutout;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  if (tex.a < Cutout) discard;\n" +
        "  vec4 light = texture2D(LightMap, LightMap0 / 240.0);\n" +
        "  gl_FragColor = tex * Color0 * vec4(ColorModulator, 1.0) * light;\n" +
        "}\n";

    // Particles vertex shader
    private static final String PARTICLES_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute float VertexAlpha;\n" +
        "attribute vec4 Color;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying float Alpha0;\n" +
        "varying vec4 Color0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Alpha0 = VertexAlpha;\n" +
        "  Color0 = Color;\n" +
        "}\n";

    private static final String PARTICLES_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying float Alpha0;\n" +
        "varying vec4 Color0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform vec3 FogColor;\n" +
        "uniform float FogStart;\n" +
        "uniform float FogEnd;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  if (tex.a * Alpha0 < 0.1) discard;\n" +
        "  gl_FragColor = tex * Color0 * Alpha0;\n" +
        "}\n";

    // Simple position + texture shader
    private static final String POSITION_TEX_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "}\n";

    private static final String POSITION_TEX_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform vec4 Color;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(Texture, TexCoord0) * Color;\n" +
        "}\n";

    // Position + texture + color shader
    private static final String POSITION_TEX_COL_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute vec4 Color;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Color0 = Color;\n" +
        "}\n";

    private static final String POSITION_TEX_COL_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec4 Color0;\n" +
        "uniform sampler2D Texture;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(Texture, TexCoord0) * Color0;\n" +
        "}\n";

    // Position + color shader
    private static final String POSITION_COLOR_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec4 Color;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec4 Color0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * vec4(Position, 1.0);\n" +
        "  Color0 = Color;\n" +
        "}\n";

    private static final String POSITION_COLOR_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec4 Color0;\n" +
        "void main() {\n" +
        "  gl_FragColor = Color0;\n" +
        "}\n";

    // Position + color + lightmap shader
    private static final String POSITION_COLOR_LIGHTMAP_VERTEX = POSITION_COLOR_VERTEX;

    private static final String POSITION_COLOR_LIGHTMAP_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec4 Color0;\n" +
        "uniform sampler2D LightMap;\n" +
        "uniform float TextureAtlasSize;\n" +
        "void main() {\n" +
        "  vec4 light = texture2D(LightMap, gl_FragCoord.xy / 256.0);\n" +
        "  gl_FragColor = Color0 * light;\n" +
        "}\n";

    // Position + texture + lightmap shader
    private static final String POSITION_TEX_LIGHTMAP_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute vec2 LightMapCoord;\n" +
        "uniform mat4 ProjMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec2 LightMapCoord0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  LightMapCoord0 = LightMapCoord;\n" +
        "}\n";

    private static final String POSITION_TEX_LIGHTMAP_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying vec2 LightMapCoord0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform sampler2D LightMap;\n" +
        "uniform vec4 Color;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  vec4 light = texture2D(LightMap, LightMapCoord0 / 240.0);\n" +
        "  gl_FragColor = tex * Color * light;\n" +
        "}\n";

    // Sky shader
    private static final String SKY_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "uniform mat4 ProjMat;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "varying float YCoord;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  YCoord = Position.y;\n" +
        "}\n";

    private static final String SKY_FRAGMENT =
        "precision mediump float;\n" +
        "varying float YCoord;\n" +
        "uniform vec3 SkyColor;\n" +
        "uniform float SkyDarken;\n" +
        "uniform float Stars;\n" +
        "void main() {\n" +
        "  float gradient = 1.0 - YCoord;\n" +
        "  vec3 color = mix(SkyColor, vec3(0.0, 0.0, 0.0), gradient * SkyDarken);\n" +
        "  gl_FragColor = vec4(color, 1.0);\n" +
        "}\n";

    // Clouds shader
    private static final String CLOUDS_VERTEX =
        "precision mediump float;\n" +
        "attribute vec3 Position;\n" +
        "attribute vec2 TexCoord;\n" +
        "attribute float Brightness;\n" +
        "uniform mat4 ProjMat;\n" +
        "uniform mat4 ModelViewMat;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying float Brightness0;\n" +
        "void main() {\n" +
        "  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n" +
        "  TexCoord0 = TexCoord;\n" +
        "  Brightness0 = Brightness;\n" +
        "}\n";

    private static final String CLOUDS_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 TexCoord0;\n" +
        "varying float Brightness0;\n" +
        "uniform sampler2D Texture;\n" +
        "uniform vec3 Color;\n" +
        "void main() {\n" +
        "  vec4 tex = texture2D(Texture, TexCoord0);\n" +
        "  if (tex.a < 0.1) discard;\n" +
        "  gl_FragColor = vec4(Color * Brightness0, tex.a);\n" +
        "}\n";
}
