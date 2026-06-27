// WebMC Socket Redirect — bridges Minecraft's TCP/HTTP to WebSocket.
//
// Two classes:
//   WebSocketSocket  — TCP-like socket API over a single WebSocket.
//   SocketRedirect   — monkey-patches the game's network layer so every
//                      java.net.Socket / java.net.HttpURLConnection is
//                      routed through a WebSocket.

/* ── WebSocketSocket ─────────────────────────────────────────── */

class WebSocketSocket {
  constructor() {
    this.ws = null;
    this.connected = false;

    this.onopen = null;
    this.onmessage = null;
    this.onclose = null;
    this.onerror = null;
  }

  connect(host, port) {
    const url = `ws://${host}:${port}`;
    this.ws = new WebSocket(url);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onopen = () => {
      this.connected = true;
      if (this.onopen) this.onopen();
    };

    this.ws.onmessage = (event) => {
      if (this.onmessage) {
        this.onmessage(event.data);
      }
    };

    this.ws.onclose = (event) => {
      this.connected = false;
      if (this.onclose) this.onclose(event.code, event.reason);
    };

    this.ws.onerror = (event) => {
      if (this.onerror) this.onerror(event);
    };
  }

  send(data) {
    if (this.connected && this.ws) this.ws.send(data);
  }

  close() {
    if (this.ws) { this.ws.close(); this.ws = null; }
    this.connected = false;
  }

  setReceiveBufferSize(size) { /* no-op */ }
  setSendBufferSize(size) { /* no-op */ }
}

/* ── SocketRedirect ──────────────────────────────────────────── */
// Replaces the game's default socket factory so Minecraft's networking
// code talks to a WebSocket instead of a real TCP socket.

class SocketRedirect {
  constructor() {
    this.wsUrl = 'ws://localhost:8080';
    this.socketMap = new Map();
    this._nextId = 0;
  }

  init(opts = {}) {
    this.wsUrl = opts.wsUrl || 'ws://localhost:8080';
    window._socketRedirect = this;
    console.log('[SocketRedirect] Init wsUrl:', this.wsUrl);
  }

  createSocket(socketName) {
    const sock = new WebSocketSocket();
    const url = new URL(this.wsUrl);
    sock.connect(url.hostname || 'localhost', url.port || 8080);
    this.socketMap.set(socketName, sock);
    this.socketMap.set(this._nextId, sock);
    return sock;
  }

  getSocket(name) {
    return this.socketMap.get(name) || this.socketMap.get(parseInt(name));
  }

  removeSocket(name) {
    const sock = this.socketMap.get(name);
    if (sock) sock.close();
    this.socketMap.delete(name);
    this.socketMap.delete(parseInt(name));
  }

  shutdown() {
    for (const [, sock] of this.socketMap) sock.close();
    this.socketMap.clear();
  }
}

// Export
if (typeof window !== 'undefined') {
  window.WebSocketSocket = WebSocketSocket;
  window.SocketRedirect = SocketRedirect;
}
