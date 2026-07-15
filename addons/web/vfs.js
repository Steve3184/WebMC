/**
 * VFS (Virtual File System) for WebMC
 * Downloads gzip-compressed MCVF package, decompresses, keeps in memory
 *
 * MCVF format:
 *   "MCVF" magic (4 bytes)
 *   version: u32LE (=1)
 *   count: u32LE
 *   for each file:
 *     path_len: u16LE
 *     path: bytes (utf-8)
 *     data_len: u32LE
 *     data: bytes
 */
(function () {
  'use strict';

  var VFS = {
    _files: null,
    _data: null,
    _ready: false,
    _onProgress: null,

    init: function (url, onReady, onProgress) {
      var self = this;
      self._onProgress = onProgress || null;

      if (self._ready) {
        if (onReady) onReady();
        return;
      }

      // console.log('[VFS] Downloading VFS from', url);
      var xhr = new XMLHttpRequest();
      xhr.open('GET', url, true);
      xhr.responseType = 'arraybuffer';

      // Track download progress
      xhr.onprogress = function (e) {
        if (e.lengthComputable && self._onProgress) {
          var percent = Math.floor((e.loaded / e.total) * 100);
          self._onProgress(percent, 'Downloading assets... ' + formatBytes(e.loaded) + ' / ' + formatBytes(e.total));
        }
      };

      xhr.onload = function () {
        if (xhr.status === 200) {
          try {
            if (self._onProgress) {
              self._onProgress(100, 'Decompressing assets...');
            }
            self._parse(new Uint8Array(xhr.response));
            if (self._onProgress) {
              self._onProgress(100, 'Assets loaded');
            }
            if (onReady) onReady();
          } catch (e) {
            if (onReady) onReady(e);
          }
        } else if (onReady) {
          onReady(new Error('download failed: ' + xhr.status));
        }
      };
      xhr.onerror = function () {
        if (onReady) onReady(new Error('network error'));
      };
      xhr.send();
    },

    _parse: function (gzBytes) {
      var self = this;
      // console.log('[VFS] Decompressing gzip...');
      var data = fflate.gunzipSync(gzBytes);
      // console.log('[VFS] Decompressed to', (data.byteLength / 1048576).toFixed(1), 'MB');

      var view = new DataView(data.buffer, data.byteOffset, data.byteLength);
      var off = 0;

      var magic = String.fromCharCode(data[off], data[off+1], data[off+2], data[off+3]); off += 4;
      if (magic !== 'MCVF') throw new Error('Bad VFS magic: ' + magic);

      var version = view.getUint32(off, true); off += 4;
      if (version !== 1) throw new Error('Unsupported VFS version: ' + version);

      var count = view.getUint32(off, true); off += 4;
      // console.log('[VFS] File count:', count);

      var index = {};

      for (var i = 0; i < count; i++) {
        var pathLen = view.getUint16(off, true); off += 2;
        var path = '';
        for (var j = 0; j < pathLen; j++) {
          path += String.fromCharCode(data[off + j]);
        }
        off += pathLen;

        var dataLen = view.getUint32(off, true); off += 4;

        index[path] = { offset: off, size: dataLen };
        off += dataLen;
      }

      self._data = data;
      self._files = index;
      self._ready = true;
      // console.log('[VFS] Loaded ' + Object.keys(index).length + ' files');
    },

    readFile: function (path, cb) {
      if (!this._ready) { return cb && cb(new Error('VFS not initialized')); }
      var entry = this._files[path];
      if (!entry) { return cb && cb(new Error('not found: ' + path)); }
      var result = this._data.slice(entry.offset, entry.offset + entry.size);
      cb(null, result);
    },

    exists: function (path) {
      return this._files && !!this._files[path];
    }
  };

  function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + ' MB';
    return (bytes / 1073741824).toFixed(2) + ' GB';
  }

  window.VFS = VFS;

  if (window.VFS_URL) {
    VFS.init(window.VFS_URL, function (err) {
      if (err) console.error('[VFS] init failed:', err);
      else // console.log('[VFS] ready');
      if (window.VFS_ONREADY) window.VFS_ONREADY();
    });
  }
})();
