package top.steve3184.webmc.teavm.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Synchronous Java PNG decoder for STBImage emulation. Replaces the old
 * Canvas/createImageBitmap-based decoder, which forced every decode to
 * yield through Thread.sleep + setTimeout and stalled MC's resource
 * reload (hundreds of textures × N sleep ticks each).
 *
 * Subset supported (covers MC vanilla 1.21.x assets):
 *   - color types 0 (Gray), 2 (RGB), 3 (Palette), 4 (Gray+Alpha), 6 (RGBA)
 *   - 8-bit depth only
 *   - tRNS for palette and grayscale
 *   - non-interlaced (Adam7 throws)
 * Unsupported cases throw an IOException so the caller logs and skips.
 *
 * Output is RGBA8 packed [R,G,B,A] in a direct ByteBuffer allocated via
 * MemoryUtil.memAlloc — matches NativeImage's expectation: little-endian
 * memGetInt yields 0xAABBGGRR, which ARGB.fromABGR turns into ARGB.
 */
public final class TinyPngDecoder implements ImageDecodeBackend {

    private static final long PNG_SIG = 0x89504E470D0A1A0AL;

    private static int decodeCounter;

    private boolean flipVertically;

    @Override
    public ByteBuffer decode(ByteBuffer encoded, IntBuffer w, IntBuffer h,
                             IntBuffer channels, int desiredChannels) {
        long t0 = System.currentTimeMillis();
        try {
            Decoded d = decodeBytes(toBytes(encoded), true);
            w.put(0, d.width);
            h.put(0, d.height);
            channels.put(0, 4);
            int n = d.width * d.height * 4;
            ByteBuffer out = org.lwjgl.system.MemoryUtil.memAlloc(n);
            byte[] src = d.rgba;
            if (flipVertically) {
                int rowBytes = d.width * 4;
                for (int y = 0; y < d.height; y++) {
                    int srcRow = (d.height - 1 - y) * rowBytes;
                    out.put(src, srcRow, rowBytes);
                }
                out.position(0);
            } else {
                out.put(src, 0, n);
                out.position(0);
            }
            out.limit(n);
            int seq = ++decodeCounter;
            long dt = System.currentTimeMillis() - t0;
            if (seq % 100 == 0 || dt > 50) {
                System.err.println("[mc-probe] TinyPng decode #" + seq + " " + d.width + "x" + d.height + " ms=" + dt);
            }
            return out;
        } catch (Throwable t) {
            System.out.println("[mc-web/png] decode failed: " + t);
            return null;
        }
    }

    @Override
    public boolean info(ByteBuffer encoded, IntBuffer w, IntBuffer h, IntBuffer channels) {
        try {
            Decoded d = decodeBytes(toBytes(encoded), false);
            w.put(0, d.width);
            h.put(0, d.height);
            channels.put(0, 4);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void setFlipVertically(boolean flip) {
        this.flipVertically = flip;
    }

    private static byte[] toBytes(ByteBuffer encoded) {
        byte[] b = new byte[encoded.remaining()];
        encoded.duplicate().get(b);
        return b;
    }

    private static final class Decoded {
        final int width;
        final int height;
        final byte[] rgba;
        Decoded(int w, int h, byte[] px) { this.width = w; this.height = h; this.rgba = px; }
    }

    private static Decoded decodeBytes(byte[] data, boolean wantPixels) throws IOException {
        if (data.length < 8) throw new IOException("PNG too short");
        long sig = readLong(data, 0);
        if (sig != PNG_SIG) throw new IOException("not a PNG (sig=" + Long.toHexString(sig) + ")");

        int width = 0, height = 0, bitDepth = 0, colorType = 0, interlace = 0;
        boolean haveIhdr = false;
        byte[] palette = null;
        byte[] trns = null;
        java.io.ByteArrayOutputStream idat = new java.io.ByteArrayOutputStream();

        int p = 8;
        while (p + 12 <= data.length) {
            int len = readInt(data, p);
            int type = readInt(data, p + 4);
            int dataOff = p + 8;
            if (len < 0 || dataOff + len + 4 > data.length) {
                throw new IOException("truncated chunk at " + p);
            }
            switch (type) {
                case 0x49484452: // IHDR
                    if (len != 13) throw new IOException("bad IHDR length");
                    width     = readInt(data, dataOff);
                    height    = readInt(data, dataOff + 4);
                    bitDepth  = data[dataOff + 8] & 0xff;
                    colorType = data[dataOff + 9] & 0xff;
                    interlace = data[dataOff + 12] & 0xff;
                    haveIhdr = true;
                    break;
                case 0x504C5445: // PLTE
                    palette = new byte[len];
                    System.arraycopy(data, dataOff, palette, 0, len);
                    break;
                case 0x74524E53: // tRNS
                    trns = new byte[len];
                    System.arraycopy(data, dataOff, trns, 0, len);
                    break;
                case 0x49444154: // IDAT
                    idat.write(data, dataOff, len);
                    break;
                case 0x49454E44: // IEND
                    p = data.length;
                    break;
                default:
                    break;
            }
            p = dataOff + len + 4; // skip data + CRC
        }

        if (!haveIhdr) throw new IOException("missing IHDR");
        if (interlace != 0) throw new IOException("interlaced PNGs not supported");
        boolean depthOk =
            bitDepth == 8
            || (colorType == 3 && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4))
            || (colorType == 0 && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4));
        if (!depthOk) {
            throw new IOException("unsupported bit depth " + bitDepth + " for color type " + colorType);
        }
        if (!wantPixels) {
            return new Decoded(width, height, null);
        }

        int channels;
        switch (colorType) {
            case 0: channels = 1; break;
            case 2: channels = 3; break;
            case 3: channels = 1; break;
            case 4: channels = 2; break;
            case 6: channels = 4; break;
            default: throw new IOException("unknown color type " + colorType);
        }

        boolean subByte = (colorType == 3 || colorType == 0) && bitDepth < 8;
        int rowBytes;
        if (subByte) {
            rowBytes = (width * bitDepth + 7) / 8;
        } else {
            rowBytes = width * channels;
        }
        int bpp = subByte ? 1 : channels;
        int filteredRowLen = rowBytes + 1;
        int totalFiltered = filteredRowLen * height;

        byte[] inflated = new byte[totalFiltered];
        inflateExact(idat.toByteArray(), inflated);

        byte[] unfiltered = unfilter(inflated, width, height, rowBytes, bpp);

        byte[] out = new byte[width * height * 4];
        switch (colorType) {
            case 6: // RGBA8
                System.arraycopy(unfiltered, 0, out, 0, out.length);
                break;
            case 2: // RGB8 → RGBA
                expandRgb(unfiltered, out, width, height, trns);
                break;
            case 0: // Gray (1/2/4/8-bit)
                expandGray(unfiltered, out, width, height, bitDepth, trns);
                break;
            case 4: // Gray+Alpha
                expandGrayAlpha(unfiltered, out, width, height);
                break;
            case 3: // Palette
                expandPalette(unfiltered, out, width, height, bitDepth, palette, trns);
                break;
        }
        return new Decoded(width, height, out);
    }

    private static void inflateExact(byte[] zlib, byte[] dst) throws IOException {
        Inflater inf = new Inflater();
        try {
            inf.setInput(zlib);
            int off = 0;
            while (off < dst.length) {
                int n;
                try {
                    n = inf.inflate(dst, off, dst.length - off);
                } catch (DataFormatException e) {
                    throw new IOException("zlib: " + e.getMessage());
                }
                if (n == 0) {
                    if (inf.finished() || inf.needsDictionary()) break;
                    if (inf.needsInput()) throw new IOException("zlib: truncated");
                }
                off += n;
            }
            if (off != dst.length) {
                throw new IOException("zlib: produced " + off + " expected " + dst.length);
            }
        } finally {
            inf.end();
        }
    }

    private static byte[] unfilter(byte[] src, int width, int height, int rowBytes, int bpp) throws IOException {
        byte[] out = new byte[rowBytes * height];
        byte[] prev = new byte[rowBytes];
        int srcOff = 0;
        for (int y = 0; y < height; y++) {
            int filter = src[srcOff++] & 0xff;
            int dstOff = y * rowBytes;
            switch (filter) {
                case 0: // None
                    System.arraycopy(src, srcOff, out, dstOff, rowBytes);
                    break;
                case 1: // Sub
                    for (int i = 0; i < rowBytes; i++) {
                        int a = i >= bpp ? out[dstOff + i - bpp] & 0xff : 0;
                        out[dstOff + i] = (byte)((src[srcOff + i] & 0xff) + a);
                    }
                    break;
                case 2: // Up
                    for (int i = 0; i < rowBytes; i++) {
                        int b = prev[i] & 0xff;
                        out[dstOff + i] = (byte)((src[srcOff + i] & 0xff) + b);
                    }
                    break;
                case 3: // Average
                    for (int i = 0; i < rowBytes; i++) {
                        int a = i >= bpp ? out[dstOff + i - bpp] & 0xff : 0;
                        int b = prev[i] & 0xff;
                        out[dstOff + i] = (byte)((src[srcOff + i] & 0xff) + ((a + b) >>> 1));
                    }
                    break;
                case 4: // Paeth
                    for (int i = 0; i < rowBytes; i++) {
                        int a = i >= bpp ? out[dstOff + i - bpp] & 0xff : 0;
                        int b = prev[i] & 0xff;
                        int c = i >= bpp ? prev[i - bpp] & 0xff : 0;
                        out[dstOff + i] = (byte)((src[srcOff + i] & 0xff) + paeth(a, b, c));
                    }
                    break;
                default:
                    throw new IOException("bad filter " + filter + " row " + y);
            }
            System.arraycopy(out, dstOff, prev, 0, rowBytes);
            srcOff += rowBytes;
        }
        return out;
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) return a;
        if (pb <= pc) return b;
        return c;
    }

    private static void expandRgb(byte[] src, byte[] dst, int w, int h, byte[] trns) {
        int trnsR = -1, trnsG = -1, trnsB = -1;
        if (trns != null && trns.length >= 6) {
            trnsR = trns[1] & 0xff;
            trnsG = trns[3] & 0xff;
            trnsB = trns[5] & 0xff;
        }
        int n = w * h;
        for (int i = 0; i < n; i++) {
            int r = src[i * 3]     & 0xff;
            int g = src[i * 3 + 1] & 0xff;
            int b = src[i * 3 + 2] & 0xff;
            dst[i * 4]     = (byte)r;
            dst[i * 4 + 1] = (byte)g;
            dst[i * 4 + 2] = (byte)b;
            dst[i * 4 + 3] = (r == trnsR && g == trnsG && b == trnsB) ? 0 : (byte)0xff;
        }
    }

    private static void expandGray(byte[] src, byte[] dst, int w, int h, int bitDepth, byte[] trns) {
        int key = (trns != null && trns.length >= 2) ? (trns[1] & 0xff) : -1;
        if (bitDepth == 8) {
            int n = w * h;
            for (int i = 0; i < n; i++) {
                int v = src[i] & 0xff;
                dst[i * 4]     = (byte)v;
                dst[i * 4 + 1] = (byte)v;
                dst[i * 4 + 2] = (byte)v;
                dst[i * 4 + 3] = (v == key) ? 0 : (byte)0xff;
            }
            return;
        }
        int rowBytes = (w * bitDepth + 7) / 8;
        int mask = (1 << bitDepth) - 1;
        int scale = 255 / mask; // 1->255, 2->85, 4->17
        for (int y = 0; y < h; y++) {
            int rowOff = y * rowBytes;
            for (int x = 0; x < w; x++) {
                int bitPos = x * bitDepth;
                int b = src[rowOff + (bitPos >>> 3)] & 0xff;
                int shift = 8 - bitDepth - (bitPos & 7);
                int raw = (b >>> shift) & mask;
                int v = raw * scale;
                int dstOff = (y * w + x) * 4;
                dst[dstOff]     = (byte)v;
                dst[dstOff + 1] = (byte)v;
                dst[dstOff + 2] = (byte)v;
                dst[dstOff + 3] = (raw == key) ? 0 : (byte)0xff;
            }
        }
    }

    private static void expandGrayAlpha(byte[] src, byte[] dst, int w, int h) {
        int n = w * h;
        for (int i = 0; i < n; i++) {
            int v = src[i * 2] & 0xff;
            int a = src[i * 2 + 1] & 0xff;
            dst[i * 4]     = (byte)v;
            dst[i * 4 + 1] = (byte)v;
            dst[i * 4 + 2] = (byte)v;
            dst[i * 4 + 3] = (byte)a;
        }
    }

    private static void expandPalette(byte[] src, byte[] dst, int w, int h, int bitDepth, byte[] palette, byte[] trns) throws IOException {
        if (palette == null) throw new IOException("palette PNG missing PLTE");
        int paletteEntries = palette.length / 3;
        int trnsLen = trns == null ? 0 : trns.length;
        int rowBytes = (w * bitDepth + 7) / 8;
        int mask = (1 << bitDepth) - 1;
        for (int y = 0; y < h; y++) {
            int rowOff = y * rowBytes;
            for (int x = 0; x < w; x++) {
                int idx;
                if (bitDepth == 8) {
                    idx = src[rowOff + x] & 0xff;
                } else {
                    int bitPos = x * bitDepth;
                    int b = src[rowOff + (bitPos >>> 3)] & 0xff;
                    int shift = 8 - bitDepth - (bitPos & 7);
                    idx = (b >>> shift) & mask;
                }
                if (idx >= paletteEntries) idx = 0;
                int dstOff = (y * w + x) * 4;
                dst[dstOff]     = palette[idx * 3];
                dst[dstOff + 1] = palette[idx * 3 + 1];
                dst[dstOff + 2] = palette[idx * 3 + 2];
                dst[dstOff + 3] = (idx < trnsLen) ? trns[idx] : (byte)0xff;
            }
        }
    }

    private static int readInt(byte[] b, int o) {
        return ((b[o] & 0xff) << 24) | ((b[o + 1] & 0xff) << 16) | ((b[o + 2] & 0xff) << 8) | (b[o + 3] & 0xff);
    }

    private static long readLong(byte[] b, int o) {
        return ((long)readInt(b, o) << 32) | (readInt(b, o + 4) & 0xffffffffL);
    }
}
