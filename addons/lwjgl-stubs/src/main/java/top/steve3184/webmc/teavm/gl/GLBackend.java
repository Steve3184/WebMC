package top.steve3184.webmc.teavm.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Pure-Java interface that the LWJGL OpenGL stubs delegate to.
 * Implemented in teavm-runtime over WebGL2RenderingContext via TeaVM JSO.
 *
 * Naming: methods are kept close to GL function names to ease search/replace
 * during patching. Argument types use plain Java (int, ByteBuffer) — the JSO
 * boundary in teavm-runtime translates ByteBuffer to TypedArrays.
 *
 * This interface is intentionally INCOMPLETE. Functions are added as MC's call
 * sites are encountered. Each function lives in a category section below.
 */
public interface GLBackend {

    // ---- Capabilities / context ----
    /** Returns a static string for {@code GL_VERSION}, etc. */
    String getString(int pname);

    // ---- Buffer objects (GL15, GL30, GL31) ----
    int  genBuffer();
    void deleteBuffer(int id);
    void bindBuffer(int target, int id);
    void bufferData(int target, ByteBuffer data, int usage);
    void bufferSubData(int target, long offset, ByteBuffer data);
    void bindBufferRange(int target, int index, int buffer, long offset, long size);

    // ---- Vertex Array Objects (GL30) ----
    int  genVertexArray();
    void deleteVertexArray(int id);
    void bindVertexArray(int id);
    void enableVertexAttribArray(int index);
    void disableVertexAttribArray(int index);
    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset);
    void vertexAttribIPointer(int index, int size, int type, int stride, long offset);
    void vertexAttribDivisor(int index, int divisor);

    // ---- Textures (GL11..GL33) ----
    int  genTexture();
    void deleteTexture(int id);
    void bindTexture(int target, int id);
    void activeTexture(int unit);
    void texImage2D(int target, int level, int internalformat, int width, int height,
                    int border, int format, int type, ByteBuffer pixels);
    void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
                       int format, int type, ByteBuffer pixels);
    void texParameteri(int target, int pname, int param);
    void texParameterf(int target, int pname, float param);
    void generateMipmap(int target);
    void pixelStorei(int pname, int param);

    // ---- Framebuffers (GL30) ----
    int  genFramebuffer();
    void deleteFramebuffer(int id);
    void bindFramebuffer(int target, int id);
    void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level);
    int  checkFramebufferStatus(int target);
    void drawBuffers(IntBuffer bufs);

    // ---- Renderbuffers (GL30) ----
    int  genRenderbuffer();
    void deleteRenderbuffer(int id);
    void bindRenderbuffer(int target, int id);
    void renderbufferStorage(int target, int internalformat, int width, int height);
    void framebufferRenderbuffer(int target, int attachment, int rbtarget, int rb);

    // ---- Shaders / Programs (GL20, GL31, GL32) ----
    int  createShader(int type);
    void deleteShader(int shader);
    void shaderSource(int shader, CharSequence source);
    void compileShader(int shader);
    int  getShaderi(int shader, int pname);
    String getShaderInfoLog(int shader);

    int  createProgram();
    void deleteProgram(int program);
    void attachShader(int program, int shader);
    void linkProgram(int program);
    void useProgram(int program);
    int  getProgrami(int program, int pname);
    String getProgramInfoLog(int program);
    int  getUniformLocation(int program, CharSequence name);
    int  getAttribLocation(int program, CharSequence name);
    int  getUniformBlockIndex(int program, CharSequence name);
    void uniformBlockBinding(int program, int blockIdx, int binding);

    // ---- Uniforms ----
    void uniform1i(int loc, int v);
    void uniform1f(int loc, float v);
    void uniform2f(int loc, float a, float b);
    void uniform3f(int loc, float a, float b, float c);
    void uniform4f(int loc, float a, float b, float c, float d);
    void uniformMatrix4fv(int loc, boolean transpose, FloatBuffer mat);

    // ---- State ----
    void viewport(int x, int y, int w, int h);
    void scissor(int x, int y, int w, int h);
    void enable(int cap);
    void disable(int cap);
    void clear(int mask);
    void clearColor(float r, float g, float b, float a);
    void clearDepthf(float d);
    void colorMask(boolean r, boolean g, boolean b, boolean a);
    void depthMask(boolean flag);
    void depthFunc(int func);
    void blendFunc(int src, int dst);
    void blendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA);
    void blendEquation(int mode);
    void cullFace(int mode);
    void frontFace(int mode);
    void polygonOffset(float factor, float units);

    // ---- Drawing ----
    void drawArrays(int mode, int first, int count);
    void drawArraysInstanced(int mode, int first, int count, int primCount);
    void drawElements(int mode, int count, int type, long offsetOrIndices);
    void drawElementsInstanced(int mode, int count, int type, long offset, int primCount);

    // ---- Read-back ----
    void readPixels(int x, int y, int w, int h, int format, int type, ByteBuffer pixels);

    // ---- Fence / sync (GL32) ----
    long fenceSync(int condition, int flags);
    int  clientWaitSync(long sync, int flags, long timeout);
    void deleteSync(long sync);

    // Add functions here as MC's compile errors require.
}
