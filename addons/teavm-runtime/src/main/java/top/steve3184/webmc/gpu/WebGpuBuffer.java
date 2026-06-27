package top.steve3184.webmc.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLBuffer;
import top.steve3184.webmc.teavm.gl.WebGLContextHolder;

import java.nio.ByteBuffer;

/**
 * Phase-2 buffer — backed by a real {@link WebGLBuffer}.
 *
 * <p>Binding target is derived from the usage flags: USAGE_VERTEX → ARRAY_BUFFER,
 * USAGE_INDEX → ELEMENT_ARRAY_BUFFER, USAGE_UNIFORM → UNIFORM_BUFFER. Anything
 * else defaults to ARRAY_BUFFER (upload-only scratch). This mirrors MC's
 * {@code com.mojang.blaze3d.opengl.GlBuffer} target selection.</p>
 */
public final class WebGpuBuffer extends GpuBuffer {

    private final String labelHint;
    private final int glTarget;
    private final int glAllocSize;
    private WebGLBuffer glHandle;
    private boolean closed = false;

    private ByteBuffer lastWrite;

    public WebGpuBuffer(String labelHint, int usage, int size) {
        super(usage, size);
        this.labelHint = labelHint;
        this.glTarget = deriveTarget(usage);
        // std140 quirk: Mojang's Std140SizeCalculator does not round the block
        // total to 16, but GL drivers do. Over-allocate the GL buffer so a
        // bindBufferRange(length=driver-reported-block-size) never overruns.
        int alloc = ((usage & USAGE_UNIFORM) != 0)
            ? ((size + 15) & ~15)
            : size;
        this.glAllocSize = alloc;
        if (WebGLContextHolder.isInstalled()) {
            WebGL2RenderingContext gl = WebGLContextHolder.gl();
            this.glHandle = gl.createBuffer();
            if (this.glHandle != null && alloc > 0) {
                gl.bindBuffer(glTarget, glHandle);
                gl.bufferData(glTarget, alloc, glUsage(usage));
            }
        }
    }

    public int glAllocSize() { return glAllocSize; }

    private static int deriveTarget(int usage) {
        if ((usage & USAGE_INDEX) != 0)   return WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER;
        if ((usage & USAGE_UNIFORM) != 0) return WebGL2RenderingContext.UNIFORM_BUFFER;
        if ((usage & USAGE_VERTEX) != 0)  return WebGL2RenderingContext.ARRAY_BUFFER;
        return WebGL2RenderingContext.ARRAY_BUFFER;
    }

    private static int glUsage(int usage) {
        if ((usage & USAGE_MAP_WRITE) != 0) return WebGL2RenderingContext.DYNAMIC_DRAW;
        return WebGL2RenderingContext.STATIC_DRAW;
    }

    public int glTarget() { return glTarget; }
    public WebGLBuffer glHandle() { return glHandle; }

    @Override public boolean isClosed() { return closed; }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (glHandle != null && WebGLContextHolder.isInstalled()) {
            WebGLContextHolder.gl().deleteBuffer(glHandle);
        }
        glHandle = null;
    }

    private static int uploadCount = 0;
    private static long uploadBytes = 0;
    public static int getUploadCount() { return uploadCount; }
    public static long getUploadBytes() { return uploadBytes; }

    /** CPU→GPU byte upload. Called by {@code WebCommandEncoder.writeToBuffer}. */
    public void uploadAt(int offset, ByteBuffer data) {
        if (data == null || glHandle == null) return;
        int n = data.remaining();
        if (n == 0) return;
        uploadCount++;
        uploadBytes += n;
        if (uploadCount <= 20) {
            String targetName = (glTarget == WebGL2RenderingContext.ARRAY_BUFFER) ? "VBO"
                : (glTarget == WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER) ? "IBO"
                : (glTarget == WebGL2RenderingContext.UNIFORM_BUFFER) ? "UBO" : "?";
            System.err.println("[mc-web/diag] uploadAt #" + uploadCount
                + " target=" + targetName + " size=" + n + " offset=" + offset
                + " label=" + labelHint);
        }
        WebGL2RenderingContext gl = WebGLContextHolder.gl();
        gl.bindBuffer(glTarget, glHandle);
        org.teavm.jso.typedarrays.Int8Array view;
        if (data.isDirect()) {
            view = org.teavm.jso.typedarrays.Int8Array.fromJavaBuffer(data.duplicate());
        } else {
            byte[] copy = new byte[n];
            data.duplicate().get(copy);
            view = org.teavm.jso.typedarrays.Int8Array.fromJavaArray(copy);
        }
        gl.bufferSubData(glTarget, offset, view);
    }

    /**
     * Return a CPU-side scratch {@link ByteBuffer} view of size {@code length}
     * starting at {@code offset}. The returned view is closed by
     * {@link WebMappedView#close()} which pushes the dirty range to GL.
     */
    public ByteBuffer mappedBuffer(int offset, int length) {
        if (lastWrite == null || lastWrite.capacity() < offset + length) {
            ByteBuffer grown = ByteBuffer.allocate(Math.max(offset + length, 1));
            if (lastWrite != null) {
                ByteBuffer old = lastWrite.duplicate();
                old.position(0);
                grown.put(old);
            }
            lastWrite = grown;
        }
        ByteBuffer dup = lastWrite.duplicate();
        dup.position(offset).limit(offset + length);
        return dup.slice();
    }

    public static final class WebMappedView implements GpuBuffer.MappedView {
        private final WebGpuBuffer owner;
        private final int offset;
        private final int length;
        private final ByteBuffer view;
        public WebMappedView(WebGpuBuffer owner, int offset, int length, ByteBuffer view) {
            this.owner = owner;
            this.offset = offset;
            this.length = length;
            this.view = view;
        }
        public WebMappedView(ByteBuffer view) {
            this.owner = null;
            this.offset = 0;
            this.length = view.remaining();
            this.view = view;
        }
        @Override public ByteBuffer data() { return view; }
        private static int closeCount = 0;
        @Override public void close() {
            closeCount++;
            if (closeCount <= 20) {
                // Check if view actually has non-zero data
                ByteBuffer check = view.duplicate();
                check.position(0);
                int nonZero = 0;
                int checkLen = Math.min(check.remaining(), 64);
                StringBuilder first16 = new StringBuilder();
                for (int i = 0; i < checkLen; i++) {
                    byte b = check.get();
                    if (b != 0) nonZero++;
                    if (i < 16) first16.append(b & 0xFF).append(",");
                }
                String targetName = (owner != null)
                    ? ((owner.glTarget == WebGL2RenderingContext.ARRAY_BUFFER) ? "VBO"
                       : (owner.glTarget == WebGL2RenderingContext.ELEMENT_ARRAY_BUFFER) ? "IBO"
                       : (owner.glTarget == WebGL2RenderingContext.UNIFORM_BUFFER) ? "UBO" : "?")
                    : "null-owner";
                System.err.println("[mc-web/diag] MappedView.close #" + closeCount
                    + " target=" + targetName
                    + " owner=" + (owner != null ? "yes" : "NO")
                    + " glHandle=" + (owner != null && owner.glHandle != null ? "yes" : "NO")
                    + " offset=" + offset + " length=" + length
                    + " viewPos=" + view.position() + " viewLim=" + view.limit()
                    + " nonZeroIn64=" + nonZero
                    + " first16=[" + first16 + "]");
            }
            if (owner != null && owner.glHandle != null) {
                ByteBuffer dup = view.duplicate();
                dup.position(0).limit(length);
                owner.uploadAt(offset, dup);
            }
        }
    }
}
