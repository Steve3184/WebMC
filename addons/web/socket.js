/**
 * SocketRedirect for WebMC Multiplayer
 * Redirects TCP/HTTP network calls to WebSocket connections
 * Preserves original Netty behavior without modifying MC source
 *
 * Features:
 * - WebSocket server connection
 * - Player position synchronization
 * - Server list functionality
 * - Direct connect functionality
 * - Reconnection handling
 * - Heartbeat/ping system
 */
(function () {
  'use strict';

  // WebMC namespace
  var WebMC = window.WebMC || {};
  window.WebMC = WebMC;

  // Server configuration
  WebMC.serverConfig = {
    currentServer: null,
    defaultWsPath: '/webmc',
    pingInterval: null,
    reconnectAttempts: 0,
    maxReconnectAttempts: 5,
    reconnectDelay: 2000,
    heartbeatInterval: 30000,
    _ws: null
  };

  /**
   * WebSocketSocket - simulates a TCP socket over WebSocket
   */
  var WebSocketSocket = function (url) {
    this._url = url;
    this._ws = null;
    this._connected = false;
    this._binaryType = 'arraybuffer';
    this._handlers = {
      onOpen: null,
      onMessage: null,
      onClose: null,
      onError: null
    };
    this._messageQueue = [];
  };

  WebSocketSocket.prototype.connect = function () {
    var self = this;
    try {
      this._ws = new WebSocket(this._url);
      this._ws.binaryType = this._binaryType;

      this._ws.onopen = function () {
        self._connected = true;
        // Flush queued messages
        while (self._messageQueue.length > 0) {
          var msg = self._messageQueue.shift();
          self._ws.send(msg);
        }
        if (self._handlers.onOpen) self._handlers.onOpen();
      };

      this._ws.onmessage = function (e) {
        var data = e.data;
        if (self._handlers.onMessage) self._handlers.onMessage(data);
      };

      this._ws.onclose = function (e) {
        self._connected = false;
        if (self._handlers.onClose) self._handlers.onClose(e.code, e.reason || '');
      };

      this._ws.onerror = function (e) {
        if (self._handlers.onError) self._handlers.onError(e);
      };
    } catch (err) {
      console.error('[WebSocketSocket] Connection error:', err);
      if (this._handlers.onError) this._handlers.onError(err);
    }
  };

  WebSocketSocket.prototype.write = function (data) {
    if (this._ws) {
      if (this._ws.readyState === WebSocket.OPEN) {
        this._ws.send(data);
      } else {
        // Queue message for when connection opens
        this._messageQueue.push(data);
      }
    }
  };

  WebSocketSocket.prototype.close = function () {
    if (this._ws) {
      this._ws.close();
      this._ws = null;
    }
    this._connected = false;
    this._messageQueue = [];
  };

  WebSocketSocket.prototype.isConnected = function () {
    return this._connected && this._ws && this._ws.readyState === WebSocket.OPEN;
  };

  WebSocketSocket.prototype.on = function (event, handler) {
    this._handlers[event] = handler;
  };

  /**
   * SocketRedirect - monkey-patches Minecraft's network layer
   * to redirect TCP/HTTP calls to WebSocket connections
   */
  var SocketRedirect = {
    _wsBase: null,
    _redirects: {},
    _activeConnections: {},

    /**
     * Initialize SocketRedirect
     * @param {string} wsBase - Base WebSocket URL (e.g., "wss://example.com/ws")
     */
    init: function (wsBase) {
      this._wsBase = wsBase;
      console.log('[SocketRedirect] Redirecting network to', wsBase);
      this._patchNetwork();
      this._setupMultiplayerHandlers();
    },

    /**
     * Initialize multiplayer mode
     * @param {string} serverAddress - Server address to connect to
     */
    initMultiplayer: function (serverAddress) {
      WebMC.serverConfig.currentServer = serverAddress;

      // Determine WebSocket URL
      var wsUrl;
      if (serverAddress.indexOf('://') === -1) {
        // No protocol specified, assume ws://
        wsUrl = 'ws://' + serverAddress + WebMC.serverConfig.defaultWsPath;
      } else {
        // Replace http(s) with ws(s)
        wsUrl = serverAddress.replace(/^http/, 'ws').replace(/^https/, 'wss');
        // Add path if not present
        if (wsUrl.indexOf('/', wsUrl.indexOf('://') + 3) === -1) {
          wsUrl += WebMC.serverConfig.defaultWsPath;
        }
      }

      this._wsBase = wsUrl;
      console.log('[SocketRedirect] Multiplayer mode, connecting to:', wsUrl);

      this._setupMultiplayerHandlers();
      this._startHeartbeat();
    },

    /**
     * Set up multiplayer message handlers
     */
    _setupMultiplayerHandlers: function () {
      var self = this;

      // Player join handler
      WebMC.onPlayerJoin = function (data) {
        console.log('[Multiplayer] Player joined:', data.name);
        WebMC.dispatchEvent('playerJoin', data);
      };

      // Player leave handler
      WebMC.onPlayerLeave = function (data) {
        console.log('[Multiplayer] Player left:', data.playerId);
        WebMC.dispatchEvent('playerLeave', data);
      };

      // Position update handler
      WebMC.onPositionUpdate = function (data) {
        WebMC.dispatchEvent('positionUpdate', data);
      };

      // Chat message handler
      WebMC.onChatMessage = function (data) {
        console.log('[Chat]', data.sender + ':', data.content);
        // Also add to the main chat history
        if (!window.chatHistory) window.chatHistory = [];
        window.chatHistory.push({
          sender: data.sender,
          content: data.content,
          type: 'chat',
          time: Date.now()
        });
        WebMC.dispatchEvent('chatMessage', data);
      },

      // System chat message handler (for server announcements)
      WebMC.onSystemMessage = function (data) {
        console.log('[System]', data.content);
        if (!window.chatHistory) window.chatHistory = [];
        window.chatHistory.push({
          sender: null,
          content: data.content,
          type: 'system',
          time: Date.now()
        });
        WebMC.dispatchEvent('systemMessage', data);
      },

      // Server info handler
      WebMC.onServerInfo = function (data) {
        console.log('[Server] Info:', data);
        WebMC.dispatchEvent('serverInfo', data);
      };
    },

    /**
     * Start heartbeat/ping interval
     */
    _startHeartbeat: function () {
      var self = this;
      if (WebMC.serverConfig.pingInterval) {
        clearInterval(WebMC.serverConfig.pingInterval);
      }

      WebMC.serverConfig.pingInterval = setInterval(function () {
        if (self._ws && self._ws.readyState === WebSocket.OPEN) {
          self._ws.send(JSON.stringify({
            type: 'ping',
            timestamp: Date.now()
          }));
        }
      }, WebMC.serverConfig.heartbeatInterval);
    },

    /**
     * Stop heartbeat
     */
    _stopHeartbeat: function () {
      if (WebMC.serverConfig.pingInterval) {
        clearInterval(WebMC.serverConfig.pingInterval);
        WebMC.serverConfig.pingInterval = null;
      }
    },

    /**
     * Attempt to reconnect to server
     */
    _attemptReconnect: function () {
      var self = this;
      var attempts = WebMC.serverConfig.reconnectAttempts;

      if (attempts >= WebMC.serverConfig.maxReconnectAttempts) {
        console.log('[SocketRedirect] Max reconnect attempts reached');
        WebMC.dispatchEvent('reconnectFailed', { attempts: attempts });
        return;
      }

      WebMC.serverConfig.reconnectAttempts++;
      var delay = WebMC.serverConfig.reconnectDelay * Math.pow(1.5, attempts - 1);

      console.log('[SocketRedirect] Reconnecting in ' + delay + 'ms (attempt ' + WebMC.serverConfig.reconnectAttempts + ')');
      WebMC.dispatchEvent('reconnecting', { attempt: WebMC.serverConfig.reconnectAttempts, delay: delay });

      setTimeout(function () {
        if (WebMC.serverConfig.currentServer) {
          self.connectToServer(WebMC.serverConfig.currentServer, {
            onOpen: function (ws) {
              WebMC.serverConfig.reconnectAttempts = 0;
              console.log('[SocketRedirect] Reconnected successfully');
              WebMC.dispatchEvent('reconnected', {});
            }
          });
        }
      }, delay);
    },

    /**
     * Patch Minecraft's network layer to use WebSocket
     */
    _patchNetwork: function () {
      var origXHR = window.XMLHttpRequest;
      var self = this;

      window.XMLHttpRequest = function () {
        var xhr = new origXHR();
        var origOpen = xhr.open;
        var origSend = xhr.send;
        var redirectInfo = null;

        xhr.open = function (method, url) {
          if (self._shouldRedirect(url)) {
            redirectInfo = { method: method, url: url };
            console.log('[SocketRedirect] Redirecting HTTP to WS:', url);
          }
          return origOpen.apply(this, arguments);
        };

        xhr.send = function (data) {
          if (redirectInfo && self._wsBase) {
            // Send over WebSocket
            var ws = new WebSocket(self._wsBase + '?url=' +
              encodeURIComponent(redirectInfo.url) + '&' +
              'method=' + redirectInfo.method);

            ws.onopen = function () {
              ws.send(JSON.stringify({
                httpMethod: redirectInfo.method,
                httpUrl: redirectInfo.url,
                data: data
              }));
            };

            ws.onmessage = function (e) {
              try {
                var response = JSON.parse(e.data);
                // Simulate XHR response
                Object.defineProperty(xhr, 'status', { value: response.status || 200, writable: false });
                Object.defineProperty(xhr, 'statusText', { value: 'OK', writable: false });
                Object.defineProperty(xhr, 'responseText', { value: response.body || '', writable: false });
                if (xhr.onload) xhr.onload({ type: 'load' });
              } catch (err) {
                if (xhr.onerror) xhr.onerror({ type: 'error' });
              }
            };

            ws.onerror = function () {
              if (xhr.onerror) xhr.onerror({ type: 'error' });
            };

            return;
          }
          return origSend.apply(this, arguments);
        };

        return xhr;
      };

      // Preserve original XMLHttpRequest
      window.origXMLHttpRequest = origXHR;
    },

    /**
     * Check if a URL should be redirected to WebSocket
     */
    _shouldRedirect: function (url) {
      // Redirect URLs that match MC's network patterns
      return url.indexOf('https://') === 0 || url.indexOf('http://') === 0;
    },

    /**
     * Create a new WebSocketSocket for TCP connections
     */
    createSocket: function (path) {
      var wsUrl = this._wsBase + '/tcp?' + path;
      return new WebSocketSocket(wsUrl);
    },

    /**
     * Connect to a multiplayer server
     */
    connectToServer: function (address, callbacks) {
      var self = this;
      var wsUrl;

      // Determine WebSocket URL
      if (address.indexOf('://') === -1) {
        wsUrl = 'ws://' + address + WebMC.serverConfig.defaultWsPath;
      } else {
        wsUrl = address.replace(/^http/, 'ws').replace(/^https/, 'wss');
        if (wsUrl.indexOf('/', wsUrl.indexOf('://') + 3) === -1) {
          wsUrl += WebMC.serverConfig.defaultWsPath;
        }
      }

      console.log('[SocketRedirect] Connecting to:', wsUrl);

      var ws = new WebSocket(wsUrl);
      ws.binaryType = 'arraybuffer';

      ws.onopen = function () {
        console.log('[SocketRedirect] Connected');
        WebMC.serverConfig.currentServer = address;
        if (callbacks && callbacks.onOpen) callbacks.onOpen(ws);
      };

      ws.onmessage = function (e) {
        var data;
        if (typeof e.data === 'string') {
          data = e.data;
          // Try to parse as JSON for multiplayer messages
          try {
            var json = JSON.parse(data);
            self._handleMultiplayerMessage(json);
          } catch (err) {
            // Not JSON, pass as raw data
          }
        } else {
          // Binary data
          data = e.data;
        }
        if (callbacks && callbacks.onMessage) callbacks.onMessage(data);
      };

      ws.onclose = function (e) {
        console.log('[SocketRedirect] Disconnected:', e.code, e.reason);
        self._stopHeartbeat();

        // Auto-reconnect on unexpected disconnect
        if (e.code !== 1000 && WebMC.serverConfig.currentServer) {
          self._attemptReconnect();
        }

        if (callbacks && callbacks.onClose) callbacks.onClose(e.code, e.reason);
      };

      ws.onerror = function (e) {
        console.error('[SocketRedirect] Error:', e);
        if (callbacks && callbacks.onError) callbacks.onError(e);
      };

      return ws;
    },

    /**
     * Handle multiplayer protocol messages
     */
    _handleMultiplayerMessage: function (json) {
      var type = json.type;

      switch (type) {
        case 'player_join':
          if (WebMC.onPlayerJoin) WebMC.onPlayerJoin(json);
          break;
        case 'player_leave':
          if (WebMC.onPlayerLeave) WebMC.onPlayerLeave(json);
          break;
        case 'position':
          if (WebMC.onPositionUpdate) WebMC.onPositionUpdate(json);
          break;
        case 'chat':
          if (WebMC.onChatMessage) WebMC.onChatMessage(json);
          break;
        case 'system':
          if (WebMC.onSystemMessage) WebMC.onSystemMessage(json);
          break;
        case 'server_info':
          if (WebMC.onServerInfo) WebMC.onServerInfo(json);
          break;
        case 'pong':
          // Handle pong response
          var pongTimestamp = json.timestamp;
          if (pongTimestamp) {
            var latency = Date.now() - pongTimestamp;
            WebMC.dispatchEvent('pong', { latency: latency, timestamp: pongTimestamp });
          }
          break;
        case 'chat_history':
          // Handle chat history on join
          if (json.messages && Array.isArray(json.messages)) {
            console.log('[SocketRedirect] Received chat history:', json.messages.length, 'messages');
            // Store history and dispatch event
            WebMC.chatHistory = json.messages;
            WebMC.dispatchEvent('chatHistory', { messages: json.messages });
          }
          break;
        case 'server_full':
          WebMC.dispatchEvent('serverFull', json);
          break;
        case 'kick':
          WebMC.dispatchEvent('kicked', json);
          self._ws.close(1000, json.reason || 'Kicked');
          break;
        default:
          console.log('[SocketRedirect] Unknown message type:', type);
      }
    },

    /**
     * Send a player position update
     */
    sendPosition: function (x, y, z, yaw, pitch, onGround) {
      if (this._ws && this._ws.readyState === WebSocket.OPEN) {
        this._ws.send(JSON.stringify({
          type: 'position',
          x: x,
          y: y,
          z: z,
          yaw: yaw,
          pitch: pitch,
          onGround: onGround,
          timestamp: Date.now()
        }));
      }
    },

    /**
     * Send a chat message
     */
    sendChat: function (message) {
      if (this._ws && this._ws.readyState === WebSocket.OPEN) {
        this._ws.send(JSON.stringify({
          type: 'chat',
          content: message,
          timestamp: Date.now()
        }));
      }
    },

    /**
     * Get current connection state
     */
    getState: function () {
      if (!this._ws) return 'disconnected';
      switch (this._ws.readyState) {
        case WebSocket.CONNECTING: return 'connecting';
        case WebSocket.OPEN: return 'connected';
        case WebSocket.CLOSING: return 'closing';
        case WebSocket.CLOSED: return 'closed';
        default: return 'unknown';
      }
    },

    /**
     * Ping a server and return latency via callback
     * @param {string} address - Server address
     * @param {function} callback - Called with { latency, success }
     */
    pingServer: function (address, callback) {
      var self = this;
      var wsUrl;

      // Convert address to WebSocket URL
      if (address.indexOf('://') === -1) {
        wsUrl = 'ws://' + address + '/webmc';
      } else {
        wsUrl = address.replace(/^http/, 'ws').replace(/^https/, 'wss');
      }

      try {
        var pingWs = new WebSocket(wsUrl);
        var startTime = Date.now();

        pingWs.onopen = function () {
          pingWs.send(JSON.stringify({ type: 'ping', timestamp: startTime }));
        };

        pingWs.onmessage = function (e) {
          try {
            var data = JSON.parse(e.data);
            if (data.type === 'pong') {
              var latency = Date.now() - (data.timestamp || startTime);
              pingWs.close();
              if (callback) callback({ latency: latency, success: true });
            }
          } catch (err) {
            // Not JSON, treat as connection success
            pingWs.close();
            if (callback) callback({ latency: Date.now() - startTime, success: true });
          }
        };

        pingWs.onerror = function () {
          if (callback) callback({ success: false, error: 'Connection failed' });
        };

        pingWs.onclose = function () {
          // Already handled above
        };

        // Timeout after 5 seconds
        setTimeout(function () {
          if (pingWs.readyState === WebSocket.CONNECTING || pingWs.readyState === WebSocket.OPEN) {
            pingWs.close();
            if (callback) callback({ success: false, error: 'Timeout' });
          }
        }, 5000);

      } catch (err) {
        if (callback) callback({ success: false, error: err.message });
      }
    },

    /**
     * Get server info via WebSocket
     */
    getServerInfo: function (address, callback) {
      var self = this;
      var wsUrl;

      if (address.indexOf('://') === -1) {
        wsUrl = 'ws://' + address + '/webmc';
      } else {
        wsUrl = address.replace(/^http/, 'ws').replace(/^https/, 'wss');
      }

      try {
        var ws = new WebSocket(wsUrl);

        ws.onopen = function () {
          ws.send(JSON.stringify({ type: 'server_info_request' }));
        };

        ws.onmessage = function (e) {
          try {
            var data = JSON.parse(e.data);
            if (data.type === 'server_info') {
              ws.close();
              if (callback) callback({ success: true, data: data });
            }
          } catch (err) {
            // Ignore parse errors
          }
        };

        ws.onerror = function () {
          if (callback) callback({ success: false, error: 'Connection failed' });
        };

        setTimeout(function () {
          if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
            ws.close();
          }
        }, 5000);

      } catch (err) {
        if (callback) callback({ success: false, error: err.message });
      }
    }
  };

  // Simple event dispatcher
  WebMC._eventListeners = {};

  WebMC.addEventListener = function (event, callback) {
    if (!this._eventListeners[event]) {
      this._eventListeners[event] = [];
    }
    this._eventListeners[event].push(callback);
  };

  WebMC.removeEventListener = function (event, callback) {
    if (this._eventListeners[event]) {
      var index = this._eventListeners[event].indexOf(callback);
      if (index > -1) {
        this._eventListeners[event].splice(index, 1);
      }
    }
  };

  WebMC.dispatchEvent = function (event, data) {
    if (this._eventListeners[event]) {
      this._eventListeners[event].forEach(function (callback) {
        try {
          callback(data);
        } catch (err) {
          console.error('[WebMC] Event handler error:', err);
        }
      });
    }
  };

  // Export
  WebMC.SocketRedirect = SocketRedirect;
  WebMC.WebSocketSocket = WebSocketSocket;
  window.SocketRedirect = SocketRedirect;
  window.WebSocketSocket = WebSocketSocket;
})();
