/**
 * SocketRedirect for WebMC
 * Redirects TCP/HTTP network calls to WebSocket connections
 * Preserves original Netty behavior without modifying MC source
 */
(function () {
  'use strict';

  /**
   * WebSocketSocket - simulates a TCP socket over WebSocket
   */
  var WebSocketSocket = function (url) {
    this._url = url;
    this._ws = null;
    this._connected = false;
    this._handlers = {
      onOpen: null,
      onMessage: null,
      onClose: null,
      onError: null
    };
  };

  WebSocketSocket.prototype.connect = function () {
    var self = this;
    this._ws = new WebSocket(this._url);

    this._ws.onopen = function () {
      self._connected = true;
      if (self._handlers.onOpen) self._handlers.onOpen();
    };

    this._ws.onmessage = function (e) {
      if (self._handlers.onMessage) self._handlers.onMessage(e.data);
    };

    this._ws.onclose = function () {
      self._connected = false;
      if (self._handlers.onClose) self._handlers.onClose();
    };

    this._ws.onerror = function (e) {
      if (self._handlers.onError) self._handlers.onError(e);
    };
  };

  WebSocketSocket.prototype.write = function (data) {
    if (this._ws && this._ws.readyState === WebSocket.OPEN) {
      this._ws.send(data);
    }
  };

  WebSocketSocket.prototype.close = function () {
    if (this._ws) {
      this._ws.close();
    }
  };

  WebSocketSocket.prototype.on = function (event, handler) {
    if (this._handlers[event]) {
      this._handlers[event] = handler;
    }
  };

  /**
   * SocketRedirect - monkey-patches Minecraft's network layer
   * to redirect TCP/HTTP calls to WebSocket connections
   */
  var SocketRedirect = {
    _wsBase: null,
    _redirects: {},

    /**
     * Initialize SocketRedirect
     * @param {string} wsBase - Base WebSocket URL (e.g., "wss://example.com/ws")
     */
    init: function (wsBase) {
      this._wsBase = wsBase;
      // console.log('[SocketRedirect] Redirecting network to', wsBase);
      this._patchNetwork();
    },

    /**
     * Patch Minecraft's network layer to use WebSocket
     */
    _patchNetwork: function () {
      // Redirect HTTP requests (for login, session, etc.)
      var origXHR = window.XMLHttpRequest;
      var self = this;

      window.XMLHttpRequest = function () {
        var xhr = new origXHR();

        var origOpen = xhr.open;
        xhr.open = function (method, url) {
          if (self._shouldRedirect(url)) {
            // Redirect to WebSocket
            var wsUrl = self._wsBase + '?url=' + encodeURIComponent(url);
            self._redirects[xhr] = { method: method, url: url };
            // console.log('[SocketRedirect] Redirecting HTTP to WS:', url);
            // Note: For full HTTP redirect over WS, implement a protocol
            // For now, log the redirect target
          }
          return origOpen.apply(this, arguments);
        };

        var origSend = xhr.send;
        xhr.send = function (data) {
          if (self._redirects[xhr]) {
            var redirect = self._redirects[xhr];
            // Send over WebSocket
            var ws = new WebSocket(self._wsBase + '?url=' +
              encodeURIComponent(redirect.url) + '&' +
              'method=' + redirect.method);
            ws.onopen = function () {
              ws.send(JSON.stringify({ data: data }));
            };
            delete self._redirects[xhr];
            return;
          }
          return origSend.apply(this, arguments);
        };

        return xhr;
      };
    },

    /**
     * Check if a URL should be redirected to WebSocket
     */
    _shouldRedirect: function (url) {
      // Redirect URLs that match MC's network patterns
      // Examples: session server, auth server, texture pack URL, etc.
      return url.indexOf('https://') === 0 || url.indexOf('http://') === 0;
    },

    /**
     * Create a new WebSocketSocket for TCP connections
     */
    createSocket: function (path) {
      var wsUrl = this._wsBase + '/tcp?' + path;
      return new WebSocketSocket(wsUrl);
    }
  };

  window.SocketRedirect = SocketRedirect;
  window.WebSocketSocket = WebSocketSocket;
})();
