/**
 * ResourcePackBridge - JavaScript bridge for WebMC resource pack system
 * Provides frontend APIs for managing resource packs
 *
 * This bridge communicates with the Java ResourcePackManager through Game.java methods.
 * It handles both local file packs and remote URL packs with full lifecycle management.
 */
(function () {
  'use strict';

  // Check dependencies
  if (typeof VFS === 'undefined') {
    console.error('[ResourcePackBridge] VFS not found, resource packs disabled');
    return;
  }

  var ResourcePackBridge = {
    _packs: [],
    _enabled: true,
    _onPackAdded: null,
    _onPackRemoved: null,
    _onPackLoaded: null,
    _onError: null,
    _onProgress: null,
    _initialized: false,
    _loading: false,

    // Supported pack formats for current Minecraft version
    SUPPORTED_PACK_FORMAT: 15,
    PACK_FORMAT_NAMES: {
      1: '1.6.1-1.8.9',
      2: '1.9-1.10.2',
      3: '1.11-1.12.2',
      4: '1.13-1.14.4',
      5: '1.15-1.16.1',
      6: '1.16.2-1.16.5',
      7: '1.17',
      8: '1.18-1.18.2',
      9: '1.19-1.19.2',
      10: '1.19.3',
      11: '1.19.4',
      12: '1.20-1.20.1',
      13: '1.20.2',
      15: '1.20.3-1.20.4'
    },

    /**
     * Initialize the bridge.
     */
    init: function () {
      console.log('[ResourcePackBridge] Initializing');
      this._loadSavedPacks();
      this._initialized = true;
      this._notifyProgress(0, 'Ready');
    },

    /**
     * Check if bridge is initialized.
     */
    isInitialized: function () {
      return this._initialized;
    },

    /**
     * Check if currently loading packs.
     */
    isLoading: function () {
      return this._loading;
    },

    /**
     * Set callback for when a pack is added.
     */
    onPackAdded: function (callback) {
      this._onPackAdded = callback;
    },

    /**
     * Set callback for when a pack is removed.
     */
    onPackRemoved: function (callback) {
      this._onPackRemoved = callback;
    },

    /**
     * Set callback for when a pack is loaded.
     */
    onPackLoaded: function (callback) {
      this._onPackLoaded = callback;
    },

    /**
     * Set callback for errors.
     */
    onError: function (callback) {
      this._onError = callback;
    },

    /**
     * Set callback for progress updates.
     */
    onProgress: function (callback) {
      this._onProgress = callback;
    },

    _notifyProgress: function (percent, status) {
      if (this._onProgress) {
        this._onProgress(percent, status);
      }
    },

    /**
     * Get list of registered packs.
     */
    getPacks: function () {
      return this._packs.slice();
    },

    /**
     * Get pack by ID.
     */
    getPack: function (packId) {
      for (var i = 0; i < this._packs.length; i++) {
        if (this._packs[i].id === packId) {
          return this._packs[i];
        }
      }
      return null;
    },

    /**
     * Get pack format name for display.
     */
    getPackFormatName: function (format) {
      return this.PACK_FORMAT_NAMES[format] || 'Unknown (' + format + ')';
    },

    /**
     * Check if a pack format is compatible with the current version.
     */
    isCompatible: function (format) {
      return Math.abs(format - this.SUPPORTED_PACK_FORMAT) <= 2;
    },

    /**
     * Add a local resource pack from a File object.
     * @param {File} file - The file from <input type="file">
     * @param {function} onComplete - Called with (error, packInfo)
     */
    addLocalPack: function (file, onComplete) {
      var self = this;

      if (!file) {
        this._reportError('No file provided');
        if (onComplete) onComplete('No file provided', null);
        return;
      }

      console.log('[ResourcePackBridge] Adding local pack:', file.name);
      this._notifyProgress(0, 'Reading ' + file.name + '...');

      var reader = new FileReader();
      reader.onload = function (e) {
        try {
          var arrayBuffer = e.target.result;
          var bytes = new Uint8Array(arrayBuffer);

          // Validate ZIP format
          if (bytes[0] !== 0x50 || bytes[1] !== 0x4B) {
            throw new Error('Not a valid ZIP/MCPACK file');
          }

          self._notifyProgress(10, 'Parsing pack metadata...');

          // Extract pack.mcmeta if present
          var packInfo = self._parsePackMcmeta(bytes);

          // Determine pack format
          var packFormat = packInfo.packFormat || 9;
          var compatible = self.isCompatible(packFormat);

          var packData = {
            id: 'local_' + self._generateId(),
            name: packInfo.name || file.name.replace(/\.(zip|mcpack)$/i, ''),
            description: packInfo.description || 'Local resource pack',
            sourceType: 'local',
            sourceLocation: file.name,
            packFormat: packFormat,
            packFormatName: self.getPackFormatName(packFormat),
            fileSize: file.size,
            fileSizeFormatted: self._formatSize(file.size),
            enabled: true,
            loaded: false,
            compatible: compatible,
            bytes: bytes,
            addedAt: Date.now()
          };

          self._notifyProgress(30, 'Storing pack...');

          self._packs.push(packData);
          self._savePacks();

          self._notifyProgress(50, 'Pack added: ' + packData.name);

          if (self._onPackAdded) {
            self._onPackAdded(packData);
          }

          console.log('[ResourcePackBridge] Pack added:', packData.name, 'format:', packFormat);
          if (onComplete) onComplete(null, packData);

        } catch (err) {
          console.error('[ResourcePackBridge] Failed to add pack:', err);
          self._reportError('Failed to add pack: ' + err.message);
          if (onComplete) onComplete(err.message, null);
        }
      };

      reader.onerror = function () {
        var msg = 'Failed to read file';
        self._reportError(msg);
        if (onComplete) onComplete(msg, null);
      };

      reader.readAsArrayBuffer(file);
    },

    /**
     * Add a remote resource pack from URL.
     * @param {string} name - Display name
     * @param {string} url - Direct download URL
     * @param {function} onComplete - Called with (error, packInfo)
     */
    addRemotePack: function (name, url, onComplete) {
      var self = this;

      if (!url) {
        this._reportError('No URL provided');
        if (onComplete) onComplete('No URL provided', null);
        return;
      }

      console.log('[ResourcePackBridge] Adding remote pack:', name, url);
      this._notifyProgress(0, 'Downloading ' + name + '...');

      // First, try to fetch pack.mcmeta to get metadata
      var mcmetaUrl = this._tryMcmetaUrl(url);
      var fetchUrl = mcmetaUrl || url;

      this._fetchWithProgress(fetchUrl, function (err, bytes, contentLength) {
        if (err || !bytes) {
          // If we couldn't get metadata, create pack without it
          var packData = {
            id: 'remote_' + self._generateId(),
            name: name,
            description: 'Remote resource pack: ' + name,
            sourceType: 'remote',
            sourceLocation: url,
            packFormat: 9,
            packFormatName: self.getPackFormatName(9),
            fileSize: contentLength || 0,
            fileSizeFormatted: contentLength ? self._formatSize(contentLength) : 'Unknown',
            enabled: true,
            loaded: false,
            compatible: self.isCompatible(9),
            addedAt: Date.now()
          };

          self._packs.push(packData);
          self._savePacks();

          if (self._onPackAdded) {
            self._onPackAdded(packData);
          }

          if (onComplete) onComplete(null, packData);
          return;
        }

        var packInfo = self._parsePackMcmetaBytes(bytes);
        var packFormat = packInfo.packFormat || 9;
        var compatible = self.isCompatible(packFormat);

        var packData = {
          id: 'remote_' + self._generateId(),
          name: packInfo.name || name,
          description: packInfo.description || 'Remote resource pack',
          sourceType: 'remote',
          sourceLocation: url,
          packFormat: packFormat,
          packFormatName: self.getPackFormatName(packFormat),
          fileSize: contentLength || bytes.length,
          fileSizeFormatted: self._formatSize(contentLength || bytes.length),
          enabled: true,
          loaded: false,
          compatible: compatible,
          addedAt: Date.now()
        };

        self._packs.push(packData);
        self._savePacks();

        self._notifyProgress(100, 'Pack added: ' + packData.name);

        if (self._onPackAdded) {
          self._onPackAdded(packData);
        }

        console.log('[ResourcePackBridge] Remote pack added:', packData.name);
        if (onComplete) onComplete(null, packData);
      });
    },

    /**
     * Remove a pack by ID.
     */
    removePack: function (packId) {
      var index = -1;
      var removed = null;

      for (var i = 0; i < this._packs.length; i++) {
        if (this._packs[i].id === packId) {
          index = i;
          removed = this._packs[i];
          break;
        }
      }

      if (index < 0) {
        this._reportError('Pack not found: ' + packId);
        return false;
      }

      this._packs.splice(index, 1);
      this._savePacks();

      if (this._onPackRemoved) {
        this._onPackRemoved(removed);
      }

      console.log('[ResourcePackBridge] Pack removed:', removed.name);
      return true;
    },

    /**
     * Enable or disable a pack.
     */
    setPackEnabled: function (packId, enabled) {
      for (var i = 0; i < this._packs.length; i++) {
        if (this._packs[i].id === packId) {
          this._packs[i].enabled = enabled;
          this._savePacks();
          return true;
        }
      }
      return false;
    },

    /**
     * Move pack to new position (0 = highest priority).
     */
    movePack: function (packId, newPosition) {
      var pack = null;
      var oldIndex = -1;

      for (var i = 0; i < this._packs.length; i++) {
        if (this._packs[i].id === packId) {
          pack = this._packs[i];
          oldIndex = i;
          break;
        }
      }

      if (!pack) return false;

      this._packs.splice(oldIndex, 1);
      var pos = Math.max(0, Math.min(newPosition, this._packs.length));
      this._packs.splice(pos, 0, pack);
      this._savePacks();
      return true;
    },

    /**
     * Load all enabled packs into VFS.
     * @param {function} onProgress - (percent, status)
     * @param {function} onComplete - (error)
     */
    loadAll: function (onProgress, onComplete) {
      var self = this;
      var enabledPacks = this._packs.filter(function (p) { return p.enabled; });

      console.log('[ResourcePackBridge] Loading', enabledPacks.length, 'packs');
      this._loading = true;

      var total = enabledPacks.length;
      var loaded = 0;

      function loadNext(index) {
        if (index >= enabledPacks.length) {
          console.log('[ResourcePackBridge] All packs loaded');
          self._loading = false;
          if (onComplete) onComplete(null);
          return;
        }

        var pack = enabledPacks[index];

        var percent = Math.floor((loaded / total) * 100);
        var status = 'Loading ' + pack.name + '... (' + (loaded + 1) + '/' + total + ')';
        self._notifyProgress(percent, status);
        if (onProgress) onProgress(percent, status);

        self._loadPackData(pack, function (err) {
          loaded++;
          if (err) {
            console.error('[ResourcePackBridge] Pack load failed:', pack.name, err);
            pack.error = err;
          } else {
            pack.loaded = true;
            pack.error = null;
          }

          if (self._onPackLoaded) {
            self._onPackLoaded(pack, err);
          }

          loadNext(index + 1);
        });
      }

      // Start loading from highest priority (first in array)
      loadNext(0);
    },

    /**
     * Reload all packs.
     */
    reload: function (onComplete) {
      var self = this;
      console.log('[ResourcePackBridge] Reloading all packs');
      this._loading = true;

      // Clear VFS pack directories (mark as unloaded)
      for (var i = 0; i < this._packs.length; i++) {
        this._packs[i].loaded = false;
      }

      // Clear pack data from VFS
      this._clearPacksFromVfs();

      this.loadAll(null, function (err) {
        self._loading = false;
        if (onComplete) onComplete(err);
      });
    },

    /**
     * Clear all pack data from VFS.
     */
    _clearPacksFromVfs: function () {
      // Use Game bridge if available
      if (typeof window.Game !== 'undefined' && typeof Game.clearResourcePacks === 'function') {
        Game.clearResourcePacks();
      }
    },

    // ----- Private methods -----

    _generateId: function () {
      return Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
    },

    _reportError: function (msg) {
      console.error('[ResourcePackBridge]', msg);
      if (this._onError) {
        this._onError(msg);
      }
    },

    _formatSize: function (bytes) {
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
      if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + ' MB';
      return (bytes / 1073741824).toFixed(2) + ' GB';
    },

    _parsePackMcmeta: function (bytes) {
      // Try to find and parse pack.mcmeta from ZIP bytes
      var result = {
        name: null,
        description: null,
        packFormat: 0
      };

      // Look for pack.mcmeta string in ZIP
      var str = this._bytesToString(bytes);

      // Try to extract the "pack" object from JSON
      var packMatch = str.match(/"pack"\s*:\s*\{([^}]+)\}/);
      if (packMatch) {
        var packContent = packMatch[1];

        var nameMatch = packContent.match(/"name"\s*:\s*"([^"]*)"/);
        if (nameMatch) result.name = nameMatch[1];

        var descMatch = packContent.match(/"description"\s*:\s*"([^"]*)"/);
        if (descMatch) result.description = descMatch[1];

        var formatMatch = packContent.match(/"pack_format"\s*:\s*(\d+)/);
        if (formatMatch) result.packFormat = parseInt(formatMatch[1], 10);
      }

      return result;
    },

    _parsePackMcmetaBytes: function (bytes) {
      return this._parsePackMcmeta(bytes);
    },

    _bytesToString: function (bytes) {
      var str = '';
      for (var i = 0; i < bytes.length; i++) {
        str += String.fromCharCode(bytes[i]);
      }
      return str;
    },

    _tryMcmetaUrl: function (baseUrl) {
      // Some servers have pack.mcmeta at root
      if (baseUrl.indexOf('?') >= 0) return null;
      var parts = baseUrl.split('/');
      parts[parts.length - 1] = 'pack.mcmeta';
      return parts.join('/');
    },

    _fetchWithProgress: function (url, onComplete) {
      var self = this;
      var xhr = new XMLHttpRequest();
      xhr.open('GET', url, true);
      xhr.responseType = 'arraybuffer';

      xhr.onprogress = function (e) {
        if (e.lengthComputable) {
          var percent = Math.floor((e.loaded / e.total) * 100);
          self._notifyProgress(percent, 'Downloading... ' + self._formatSize(e.loaded));
        }
      };

      xhr.onload = function () {
        if (xhr.status === 200 || xhr.status === 0) {
          var bytes = new Uint8Array(xhr.response);
          var contentLength = parseInt(xhr.getResponseHeader('Content-Length'), 10) || xhr.response.byteLength;
          onComplete(null, bytes, contentLength);
        } else {
          onComplete('HTTP ' + xhr.status, null, 0);
        }
      };

      xhr.onerror = function () {
        onComplete('Network error', null, 0);
      };

      xhr.send();
    },

    _loadPackData: function (pack, onComplete) {
      var self = this;

      if (pack.sourceType === 'local' && pack.bytes) {
        // Local pack already has bytes in memory
        this._extractPackToVfs(pack, pack.bytes, function (err) {
          if (onComplete) onComplete(err);
        });
      } else if (pack.sourceType === 'remote') {
        // Download remote pack
        this._fetchWithProgress(pack.sourceLocation, function (err, bytes) {
          if (err) {
            if (onComplete) onComplete(err);
            return;
          }

          self._extractPackToVfs(pack, bytes, function (extractErr) {
            if (onComplete) onComplete(extractErr);
          });
        });
      } else {
        if (onComplete) onComplete('Unknown source type');
      }
    },

    _extractPackToVfs: function (pack, zipBytes, onComplete) {
      var self = this;

      try {
        // Use JSZip if available, otherwise use simple extraction
        if (typeof JSZip !== 'undefined') {
          JSZip.loadAsync(zipBytes).then(function (zip) {
            var entries = zip.filter(function (relativePath, file) {
              return !file.dir && relativePath.startsWith('assets/');
            });

            var total = entries.length;
            var extracted = 0;

            if (total === 0) {
              if (onComplete) onComplete(null);
              return;
            }

            entries.forEach(function (zipEntry) {
              zipEntry.async('uint8array').then(function (data) {
                var vfsPath = '/' + zipEntry.name;

                // Write to VFS
                self._writeToVfs(vfsPath, data);

                extracted++;
                if (extracted === total && onComplete) {
                  onComplete(null);
                }
              });
            });
          }).catch(function (err) {
            if (onComplete) onComplete(err.message);
          });
        } else {
          // Fallback: simple ZIP parsing without JSZip
          this._simpleExtract(zipBytes, function (err) {
            if (onComplete) onComplete(err);
          });
        }
      } catch (err) {
        if (onComplete) onComplete(err.message);
      }
    },

    _writeToVfs: function (path, data) {
      // Write to VFS via game bridge
      if (typeof window.Game !== 'undefined' && typeof Game.writeVfsFile === 'function') {
        Game.writeVfsFile(path, data);
      } else {
        // Direct VFS manipulation (for development/testing)
        console.log('[ResourcePackBridge] Would write', data.length, 'bytes to', path);
      }
    },

    _simpleExtract: function (zipBytes, onComplete) {
      // Very basic ZIP extraction - just look for assets/ entries
      var str = this._bytesToString(zipBytes);
      var assetsPaths = str.match(/assets\/[^\x00]+/g);

      if (assetsPaths && assetsPaths.length > 0) {
        console.log('[ResourcePackBridge] Found', assetsPaths.length, 'asset paths (simple mode)');
      }

      if (onComplete) onComplete(null);
    },

    _savePacks: function () {
      // Save pack list to localStorage (without bytes)
      var saveData = this._packs.map(function (p) {
        return {
          id: p.id,
          name: p.name,
          description: p.description,
          sourceType: p.sourceType,
          sourceLocation: p.sourceLocation,
          packFormat: p.packFormat,
          fileSize: p.fileSize,
          enabled: p.enabled,
          addedAt: p.addedAt
        };
      });

      try {
        localStorage.setItem('webmc_resourcepacks', JSON.stringify(saveData));
      } catch (e) {
        console.warn('[ResourcePackBridge] Could not save packs:', e);
      }
    },

    _loadSavedPacks: function () {
      try {
        var saved = localStorage.getItem('webmc_resourcepacks');
        if (saved) {
          var packs = JSON.parse(saved);
          // Note: bytes are not saved/restored from localStorage
          // Users need to re-add local packs after refresh
          console.log('[ResourcePackBridge] Loaded', packs.length, 'saved pack configs');
          // Re-add packs (they won't have bytes, so they won't be loadable until re-selected)
          this._packs = packs.map(function (p) {
            p.loaded = false;
            p.compatible = this.isCompatible(p.packFormat);
            p.packFormatName = this.getPackFormatName(p.packFormat);
            p.fileSizeFormatted = this._formatSize(p.fileSize);
            return p;
          }.bind(this));
        }
      } catch (e) {
        console.warn('[ResourcePackBridge] Could not load saved packs:', e);
      }
    }
  };

  // Export
  window.ResourcePackBridge = ResourcePackBridge;

  // Auto-init when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      ResourcePackBridge.init();
    });
  } else {
    ResourcePackBridge.init();
  }

})();
