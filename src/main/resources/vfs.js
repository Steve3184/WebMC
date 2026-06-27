// WebMC VFS - Virtual File System
// Downloads game assets, decompresses them, and serves via localStorage.
// Supports gzip (native DecompressionStream + pako fallback) and
// xz (native DecompressionStream only — requires Chrome 115+/Firefox 118+).
//
// Usage:
//   const vfs = new VFS();
//   await vfs.init('assets.tar.xz');
//   const data = await vfs.readFile('assets/texture.png');

const VFS_META_KEY = 'webmc-vfs-meta';
const VFS_FILE_PREFIX = 'webmc-file:';

// ── Base64 <-> Binary helpers ─────────────────────────────────

function _b64ToBytes(b64) {
  const bin = atob(b64);
  const arr = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
  return arr;
}

function _bytesToB64(bytes) {
  const bin = String.fromCharCode.apply(null, Array.from(bytes));
  return btoa(bin);
}

// ── Public API ────────────────────────────────────────────────

class VFS {
  constructor() {
    this.initialized = false;
  }

  /**
   * @param {string} assetUrl  URL to the xz-compressed tar archive
   */
  async init(assetUrl) {
    if (this.initialized) return;

    // Check localStorage cache
    const metaRaw = localStorage.getItem(VFS_META_KEY);
    if (metaRaw) {
      try {
        const meta = JSON.parse(metaRaw);
        if (meta.url === assetUrl) {
          this.initialized = true;
          console.log('[VFS] Using cached assets from localStorage');
          return;
        }
      } catch { /* stale/invalid meta, proceed to download */ }
    }

    console.log('[VFS] Downloading and extracting:', assetUrl);
    await this._downloadAndExtract(assetUrl);
    this.initialized = true;
  }

  readFile(path) {
    if (!this.initialized) throw new Error('VFS not initialized');
    const b64 = localStorage.getItem(VFS_FILE_PREFIX + path);
    return b64 ? _b64ToBytes(b64) : null;
  }

  async readFileToString(path) {
    const data = await this.readFile(path);
    return data ? new TextDecoder().decode(data) : null;
  }

  listFiles(prefix) {
    if (!this.initialized) throw new Error('VFS not initialized');
    const files = [];
    const prefixLen = VFS_FILE_PREFIX.length;
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith(VFS_FILE_PREFIX)) {
        const name = key.slice(prefixLen);
        if (!prefix || name.startsWith(prefix)) files.push(name);
      }
    }
    return files.sort();
  }

  // ── Download & extract ──────────────────────────────────────

  async _downloadAndExtract(assetUrl) {
    const resp = await fetch(assetUrl);
    if (!resp.ok) throw new Error(`Fetch failed: ${resp.status} ${assetUrl}`);
    const bytes = new Uint8Array(await resp.arrayBuffer());

    // Detect format
    const urlLower = assetUrl.toLowerCase();
    let isXz = urlLower.endsWith('.xz') || urlLower.endsWith('.tar.xz');
    if (!isXz && bytes.byteLength >= 6) {
      isXz = bytes[0] === 0xFD && bytes[1] === 0x37 &&
             bytes[2] === 0x7A && bytes[3] === 0x58 &&
             bytes[4] === 0x5A && bytes[5] === 0x00;
    }

    let tarData;
    if (isXz) {
      console.log('[VFS] Detected xz format');
      tarData = await this._decompressXz(bytes);
    } else {
      console.log('[VFS] Detected gzip format');
      tarData = await this._decompressGzip(bytes);
    }

    await this._extractTarAndStore(new Uint8Array(tarData));

    // Store metadata
    localStorage.setItem(VFS_META_KEY, JSON.stringify({
      url: assetUrl,
      timestamp: Date.now()
    }));
  }

  // ── Gzip decompression ──────────────────────────────────────

  async _decompressGzip(data) {
    try {
      const ds = new DecompressionStream('gzip');
      const blob = new Blob([data]);
      const stream = blob.stream().pipeThrough(ds);
      return new Uint8Array(await new Response(stream).arrayBuffer());
    } catch {
      console.warn('[VFS] Native gzip not available, loading pako...');
      if (typeof window.pako === 'undefined') {
        await this._loadScript('https://cdn.jsdelivr.net/npm/pako@2.1.0/dist/pako.min.js');
      }
      try {
        return pako.inflate(data, { to: 'string' }).buffer;
      } catch (e) {
        throw new Error('gzip decompression failed: ' + e.message);
      }
    }
  }

  // ── XZ decompression ────────────────────────────────────────

  async _decompressXz(data) {
    try {
      const ds = new DecompressionStream('xz');
      const blob = new Blob([data]);
      const stream = blob.stream().pipeThrough(ds);
      return new Uint8Array(await new Response(stream).arrayBuffer());
    } catch {
      throw new Error('xz decompression not supported — your browser needs to be upgraded (Chrome 115+, Firefox 118+, or Edge 115+)');
    }
  }

  async _loadScript(url) {
    return new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = url;
      s.onload = resolve;
      s.onerror = reject;
      document.head.appendChild(s);
    });
  }

  // ── Tar extraction ──────────────────────────────────────────

  _extractTarAndStore(tarData) {
    let offset = 0;
    let fileCount = 0;
    let totalBytes = 0;
    console.log(`[VFS] Parsing tar archive (${tarData.length} bytes)...`);

    while (offset < tarData.length) {
      if (offset + 1024 <= tarData.length) {
        const b1 = tarData.slice(offset, offset + 512);
        const b2 = tarData.slice(offset + 512, offset + 1024);
        if (b1.every(b => b === 0) && b2.every(b => b === 0)) break;
      }
      if (offset + 512 > tarData.length) break;

      const header = tarData.slice(offset, offset + 512);
      const name = this._tarReadField(header, 0, 100);
      if (!name) { offset += 512; continue; }

      const fileSize = this._tarParseOctal(header, 124, 12);
      const typeFlag = String.fromCharCode(header[156]);
      const paddedSize = Math.ceil(fileSize / 512) * 512;

      if (typeFlag === '2' || typeFlag === '5') {
        offset += 512 + paddedSize;
        continue;
      }

      if (fileSize > 0 && fileSize < 100_000_000) {
        const dataStart = offset + 512;
        const dataEnd = Math.min(dataStart + fileSize, tarData.length);
        const fileData = tarData.slice(dataStart, dataEnd);

        // Store as base64 in localStorage (synchronous)
        const b64 = _bytesToB64(fileData);
        localStorage.setItem(VFS_FILE_PREFIX + name, b64);

        fileCount++;
        totalBytes += fileData.length;
      }
      offset += 512 + paddedSize;

      if (fileCount % 200 === 0) {
        console.log(`[VFS] Extracted ${fileCount} files (${(totalBytes / 1024 / 1024).toFixed(1)} MB)`);
      }
    }

    console.log(`[VFS] Extracted ${fileCount} files, ${(totalBytes / 1024 / 1024).toFixed(1)} MB total`);
  }

  _tarReadField(header, start, length) {
    const bytes = [];
    for (let i = start; i < start + length && i < header.length; i++) {
      if (header[i] === 0 || header[i] === 32) break;
      bytes.push(header[i]);
    }
    return String.fromCharCode(...bytes).trim();
  }

  _tarParseOctal(header, start, length) {
    let value = 0;
    for (let i = start; i < start + length && i < header.length; i++) {
      const b = header[i];
      if (b === 0 || b === 32) break;
      const d = b - 48;
      if (d >= 0 && d <= 7) value = value * 8 + d;
    }
    return value;
  }
}

// Export
if (typeof window !== 'undefined') window.VFS = VFS;
if (typeof module !== 'undefined' && module.exports) module.exports = { VFS };
