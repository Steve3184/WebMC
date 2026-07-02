# LWJGL stubs

Replacement for org.lwjgl.* on the TeaVM/Web target. These classes shadow the
real LWJGL bindings at compile time so that MC compiles against our WebGL2 /
Web Audio / DOM-event implementations instead of native bindings.

## Package coverage and strategy

| Real LWJGL package | Replacement strategy | Status |
|---|---|---|
| `org.lwjgl.glfw` | Full stub: window/input/cursor/clipboard backed by DOM events from `addons/teavm-runtime`. | scaffolded |
| `org.lwjgl.openal.*` | Full stub backed by Web Audio API. | TODO |
| `org.lwjgl.stb.*` | Stub: PNG/JPG via `createImageBitmap`; Vorbis via Web Audio `decodeAudioData`; truetype via canvas measureText. | TODO |
| `org.lwjgl.system.MemoryUtil` | DirectByteBuffer-backed; pointer arithmetic via int handles. | scaffolded |
| `org.lwjgl.system.MemoryStack` | ThreadLocal stack of pre-allocated ByteBuffers. | scaffolded |
| `org.lwjgl.PointerBuffer`, `BufferUtils` | Pure-Java; no native dependency. | scaffolded |
| `org.lwjgl.opengl.*` | **Constants only.** Function bodies are NOT stubbed here. | scaffolded (constants for GL11–GL33) |
| `com.mojang.blaze3d.*` (NOT lwjgl) | Patched in `patches/` to call our WebGL2 backend instead of LWJGL GL. This is where the real GL→WebGL translation lives. | TODO (phase 2) |

## Why GL constants but not GL functions?

MC code references constants in MANY places (`GL_TEXTURE_2D`, `GL_TRIANGLES`,
`GL_FRAMEBUFFER`, ...) — they must compile. The values are dictated by the GL
spec and are identical across implementations.

MC code calls GL **functions** through `com.mojang.blaze3d.platform.GlStateManager`
and friends in 95%+ of cases. The remaining direct calls (debug overlays,
RenderTarget setup, etc.) are localized; we patch them or stub the specific
functions on demand. Stubbing all 1000+ LWJGL OpenGL functions up-front would
be wasteful.

## How a missing function is filled in

When `gradle compileJava` complains about `org.lwjgl.opengl.GL30.glXyz`:
1. Add a static method to `GL30.java` here.
2. Implement it by calling the WebGL2 backend in `addons/teavm-runtime`
   (`GLBackend.glXyz(...)` in package `top.steve3184.webmc.teavm.gl`).
3. The TeaVM JSO layer in teavm-runtime translates the call to the actual
   `WebGL2RenderingContext` method.

## Package
We **must** keep the package paths exactly `org.lwjgl.*` to shadow upstream LWJGL.
The build.gradle for the web target excludes the real LWJGL artifacts so there
is no class collision.
