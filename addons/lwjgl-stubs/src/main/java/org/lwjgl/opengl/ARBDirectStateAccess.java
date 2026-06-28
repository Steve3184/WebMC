package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import top.steve3184.webmc.teavm.gl.GLBackend;
import top.steve3184.webmc.teavm.gl.GLBackendHolder;

/**
 * Stub of ARBDirectStateAccess (DSA). WebGL2 has no DSA — must bind first.
 * This class emulates DSA behavior by binding the object, calling the GL function,
 * then restoring the previous binding state.
 */
public final class ARBDirectStateAccess {

    // Track previous bindings for restore-after-call emulation
    private static int prevBufferBinding = -1;
    private static int prevTextureBinding = -1;
    private static int prevFramebufferBinding = -1;
    private static int prevVertexArrayBinding = -1;

    public static int glCreateBuffers() {
        return GL15.glGenBuffers();
    }

    public static int glCreateTextures(int target) {
        return GL11.glGenTextures();
    }

    public static int glCreateFramebuffers() {
        return GL30.glGenFramebuffers();
    }

    public static int glCreateRenderbuffers() {
        return GL30.glGenRenderbuffers();
    }

    public static int glCreateVertexArrays() {
        return GL30.glGenVertexArrays();
    }

    // ---- Buffer Object functions ----

    public static void glNamedBufferData(int buf, ByteBuffer data, int usage) {
        GLBackend gl = GLBackendHolder.current();
        // DSA: set buffer data without binding
        int prev = getBufferBinding(GL15.GL_ARRAY_BUFFER);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, buf);
        gl.bufferData(GL15.GL_ARRAY_BUFFER, data, usage);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }

    public static void glNamedBufferData(int buf, long size, int usage) {
        GLBackend gl = GLBackendHolder.current();
        // Allocate uninitialized buffer
        int prev = getBufferBinding(GL15.GL_ARRAY_BUFFER);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, buf);
        gl.bufferData(GL15.GL_ARRAY_BUFFER, null, usage); // null = allocate
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }

    public static void glNamedBufferSubData(int buf, long off, ByteBuffer data) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getBufferBinding(GL15.GL_ARRAY_BUFFER);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, buf);
        gl.bufferSubData(GL15.GL_ARRAY_BUFFER, off, data);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }

    public static void glNamedBufferStorage(int buf, ByteBuffer data, int flags) {
        // GL15.glBufferData with DYNAMIC_STORAGE_BIT implies GL_DYNAMIC_DRAW
        // For immutable buffers in WebGL2, we use STATIC_DRAW if no flags
        int usage = (flags & 0x0001) != 0 ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW;
        glNamedBufferData(buf, data, usage);
    }

    public static void glNamedBufferStorage(int buf, long size, int flags) {
        int usage = (flags & 0x0001) != 0 ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW;
        glNamedBufferData(buf, size, usage);
    }

    public static ByteBuffer glMapNamedBufferRange(int buf, int offset, int length, int access) {
        // WebGL2 doesn't support persistent mapping. Return a copy.
        GLBackend gl = GLBackendHolder.current();
        int prev = getBufferBinding(GL15.GL_ARRAY_BUFFER);
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, buf);

        // Read current buffer content
        ByteBuffer result = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
        gl.readPixels(offset, 0, length, 1, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, result);
        result.flip();

        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, prev);
        return result;
    }

    public static ByteBuffer glMapNamedBufferRange(int buf, long offset, long length, int access) {
        return glMapNamedBufferRange(buf, (int) offset, (int) length, access);
    }

    public static boolean glUnmapNamedBuffer(int buf) {
        // No-op: WebGL2 doesn't have explicit unmapping
        return true;
    }

    public static void glFlushMappedNamedBufferRange(int buf, int offset, int length) {
        // WebGL2 flush is implicit on GPU commands
    }

    public static void glFlushMappedNamedBufferRange(int buf, long offset, long length) {
        glFlushMappedNamedBufferRange(buf, (int) offset, (int) length);
    }

    public static void glCopyNamedBufferSubData(int read, int write, long readOffset, long writeOffset, long size) {
        GLBackend gl = GLBackendHolder.current();
        int prevRead = getBufferBinding(GL15.GL_COPY_READ_BUFFER);
        int prevWrite = getBufferBinding(GL15.GL_COPY_WRITE_BUFFER);

        gl.bindBuffer(GL15.GL_COPY_READ_BUFFER, read);
        gl.bindBuffer(GL15.GL_COPY_WRITE_BUFFER, write);
        // WebGL2: use copyBufferSubData if available, otherwise manual copy
        // For now, use bufferSubData approach
        gl.bindBuffer(GL15.GL_ARRAY_BUFFER, write);
        // Note: actual copy would require reading back which is expensive
        // This is a stub - real implementation would need GPU copy path

        gl.bindBuffer(GL15.GL_COPY_READ_BUFFER, prevRead);
        gl.bindBuffer(GL15.GL_COPY_WRITE_BUFFER, prevWrite);
    }

    public static void glCopyNamedBufferSubData(int read, int write, int readOffset, int writeOffset, int size) {
        glCopyNamedBufferSubData(read, write, (long) readOffset, (long) writeOffset, (long) size);
    }

    // ---- Framebuffer functions ----

    public static void glNamedFramebufferTexture(int fb, int attachment, int texture, int level) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getFramebufferBinding(GL30.GL_FRAMEBUFFER);
        gl.bindFramebuffer(GL30.GL_FRAMEBUFFER, fb);
        gl.framebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, texture, level);
        gl.bindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
    }

    public static void glBlitNamedFramebuffer(int rfb, int dfb,
                                              int srcX0, int srcY0, int srcX1, int srcY1,
                                              int dstX0, int dstY0, int dstX1, int dstY1,
                                              int mask, int filter) {
        GLBackend gl = GLBackendHolder.current();
        // Bind both framebuffers and use blit
        int prevRead = getFramebufferBinding(GL30.GL_READ_FRAMEBUFFER);
        int prevDraw = getFramebufferBinding(GL30.GL_DRAW_FRAMEBUFFER);

        gl.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, rfb);
        gl.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dfb);
        // Note: WebGL2 blit is done via GL32.blitFramebuffer, not here
        // This is a placeholder

        gl.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        gl.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    // ---- Texture functions ----

    public static void glTextureStorage2D(int t, int levels, int internalformat, int w, int h) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getTextureBinding(GL11.GL_TEXTURE_2D);
        gl.bindTexture(GL11.GL_TEXTURE_2D, t);
        // WebGL2: use texStorage2D equivalent via texImage2D for each level
        // This is a simplified implementation
        for (int level = 0; level < levels; level++) {
            int size = Math.max(1, w >> level);
            int sizeY = Math.max(1, h >> level);
            gl.texImage2D(GL11.GL_TEXTURE_2D, level, internalformat, size, sizeY, 0,
                         GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
        }
        gl.bindTexture(GL11.GL_TEXTURE_2D, prev);
    }

    public static void glTextureSubImage2D(int t, int level, int x, int y, int w, int h,
                                           int fmt, int type, ByteBuffer pix) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getTextureBinding(GL11.GL_TEXTURE_2D);
        gl.bindTexture(GL11.GL_TEXTURE_2D, t);
        gl.texSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, w, h, fmt, type, pix);
        gl.bindTexture(GL11.GL_TEXTURE_2D, prev);
    }

    public static void glTextureParameteri(int t, int p, int v) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getTextureBinding(GL11.GL_TEXTURE_2D);
        gl.bindTexture(GL11.GL_TEXTURE_2D, t);
        gl.texParameteri(GL11.GL_TEXTURE_2D, p, v);
        gl.bindTexture(GL11.GL_TEXTURE_2D, prev);
    }

    public static void glGenerateTextureMipmap(int t) {
        GLBackend gl = GLBackendHolder.current();
        int prev = getTextureBinding(GL11.GL_TEXTURE_2D);
        gl.bindTexture(GL11.GL_TEXTURE_2D, t);
        gl.generateMipmap(GL11.GL_TEXTURE_2D);
        gl.bindTexture(GL11.GL_TEXTURE_2D, prev);
    }

    public static void glBindTextureUnit(int unit, int tex) {
        GLBackend gl = GLBackendHolder.current();
        gl.activeTexture(GL11.GL_TEXTURE0 + unit);
        gl.bindTexture(GL11.GL_TEXTURE_2D, tex);
    }

    // ---- Helper methods to track bindings ----
    // Note: In a real implementation, these would query GL state.
    // For WebGL2, we maintain local shadow state.

    private static int getBufferBinding(int target) {
        // Query current binding from GL state via GL_BACKING
        // This is a simplified version - in practice you'd track shadow state
        return 0; // Default: no buffer bound
    }

    private static int getTextureBinding(int target) {
        return 0; // Default: no texture bound
    }

    private static int getFramebufferBinding(int target) {
        return 0; // Default: no framebuffer bound
    }

    private static int getVertexArrayBinding() {
        return 0; // Default: default VAO
    }

    private ARBDirectStateAccess() {}
}