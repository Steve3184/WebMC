/**
 * Split-JS Loader for WebMC
 * Loads TeaVM output in class-based chunks for on-demand loading
 *
 * Manifest format (split-manifest.json):
 * {
 *   "main": "game.js",
 *   "chunks": {
 *     "net/minecraft/client/main/Main": "classes/net/minecraft/client/main/Main.js",
 *     ...
 *   }
 * }
 */
(function () {
  'use strict';

  var SplitJS = {
    _manifest: null,
    _loaded: {},
    _queue: [],
    _running: false,

    /**
     * Load manifest and start loading chunks
     * @param {string} manifestUrl - URL to split-manifest.json
     * @param {function} onReady - Called when main module is ready
     */
    load: function (manifestUrl, onReady) {
      var self = this;
      console.log('[SplitJS] Loading manifest from', manifestUrl);

      var xhr = new XMLHttpRequest();
      xhr.open('GET', manifestUrl, true);
      xhr.responseType = 'json';
      xhr.onload = function () {
        if (xhr.status === 200) {
          self._manifest = xhr.response;
          console.log('[SplitJS] Manifest loaded:', Object.keys(self._manifest.chunks).length, 'chunks');

          // Load main module first
          self._loadChunk(self._manifest.main, function () {
            console.log('[SplitJS] Main module loaded');
            if (onReady) onReady();
          });

          // Start processing queue
          self._processQueue();
        } else {
          console.error('[SplitJS] Failed to load manifest:', xhr.status);
        }
      };
      xhr.onerror = function () {
        console.error('[SplitJS] Network error loading manifest');
      };
      xhr.send();
    },

    /**
     * Request a class to be loaded
     * @param {string} className - Class name (e.g., "net/minecraft/client/main/Main")
     */
    requestClass: function (className) {
      var self = this;
      var chunkFile = this._manifest.chunks[className];

      if (!chunkFile) {
        console.warn('[SplitJS] No chunk found for class:', className);
        return;
      }

      if (this._loaded[chunkFile]) {
        return; // already loaded
      }

      this._queue.push({ class: className, chunk: chunkFile });
      this._processQueue();
    },

    /**
     * Process the load queue
     */
    _processQueue: function () {
      var self = this;
      if (this._running || this._queue.length === 0) return;

      this._running = true;
      var item = this._queue.shift();

      self._loadChunk(item.chunk, function () {
        self._loaded[item.chunk] = true;
        console.log('[SplitJS] Loaded chunk:', item.chunk);
        self._running = false;

        // Process next item
        self._processQueue();
      });
    },

    /**
     * Load a JavaScript chunk
     */
    _loadChunk: function (file, callback) {
      var self = this;
      var script = document.createElement('script');
      script.src = file;
      script.onload = callback;
      script.onerror = function () {
        console.error('[SplitJS] Failed to load chunk:', file);
        if (callback) callback(new Error('Failed to load chunk: ' + file));
      };
      document.head.appendChild(script);
    },

    /**
     * Check if a class has been loaded
     */
    isLoaded: function (className) {
      var chunkFile = this._manifest && this._manifest.chunks[className];
      return chunkFile ? !!this._loaded[chunkFile] : false;
    }
  };

  window.SplitJS = SplitJS;

  // Auto-load if manifestUrl is set
  if (window.SPLIT_MANIFEST_URL) {
    SplitJS.load(window.SPLIT_MANIFEST_URL, function () {
      console.log('[SplitJS] All initial chunks loaded');
      if (window.SPLIT_ONREADY) window.SPLIT_ONREADY();
    });
  }
})();
