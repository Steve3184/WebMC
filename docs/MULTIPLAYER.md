# WebMC Multiplayer Setup Guide

This guide covers setting up WebMC multiplayer, including server requirements, WebSocket bridge configuration, and troubleshooting.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Server Requirements](#server-requirements)
3. [Simple Server Setup](#simple-server-setup)
4. [WebSocket Bridge Setup](#websocket-bridge-setup)
5. [Configuration Options](#configuration-options)
6. [Client Configuration](#client-configuration)
7. [Troubleshooting](#troubleshooting)
8. [EaglerPorts Architecture Reference](#eaglerports-architecture-reference)

---

## Architecture Overview

WebMC uses a WebSocket-based multiplayer architecture with two modes:

### Local Mode (LAN Play)

```
Browser 1 ←→ WebSocket Server ←→ Browser 2
                    │
              No MC Server
              Required
              (Player sync only)
```

### Bridge Mode (Real Minecraft)

```
Browser ←→ WebSocket Bridge ←→ Minecraft Server
                │                   │
                │              (port 25565)
                │                   │
                └── TCP Proxy ──────┘
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| Simple Server | `server/server.js` | Local multiplayer |
| Bridge Server | `scripts/beta-proxy-server.js` | MC server proxy |
| Client Bridge | `work/src/.../WebSocketBridge.java` | Browser-side networking |

---

## Server Requirements

### Hardware Requirements

| Players | CPU | RAM | Disk |
|---------|-----|-----|------|
| 1-10 | 1 core | 512 MB | 1 GB |
| 10-50 | 2 cores | 1 GB | 2 GB |
| 50-100 | 4 cores | 2 GB | 5 GB |

### Software Requirements

| Component | Version | Notes |
|-----------|---------|-------|
| Node.js | 18+ | Runtime for server |
| npm | 9+ | Package management |
| OS | Any | Linux, macOS, Windows |

### Network Requirements

| Port | Protocol | Purpose |
|------|----------|---------|
| 8080 | WebSocket | Default multiplayer port |
| 25565 | TCP | Minecraft server (bridge mode) |

### Firewall Configuration

```bash
# Linux (ufw)
ufw allow 8080/tcp  # WebSocket
ufw allow 8081/tcp  # Optional: HTTP health checks

# Linux (firewalld)
firewall-cmd --add-port=8080/tcp
firewall-cmd --add-port=8081/tcp

# macOS
sudo pfctl -f /etc/pf.conf
# Add: pass in proto tcp from any to any port 8080

# Windows
# Windows Defender Firewall → New Rule → Port 8080 TCP
```

---

## Simple Server Setup

For local/LAN multiplayer without connecting to real Minecraft servers.

### 1. Install Dependencies

```bash
cd server
npm install
```

### 2. Start the Server

```bash
# Default port (8080)
npm start

# Custom port
npm start 3000

# Or run directly
node server.js 8080
```

### 3. Verify Server is Running

```bash
# Check health endpoint
curl http://localhost:8080/health

# Expected response:
# {"status":"ok","clients":0,"connectedServers":0,"uptime":10}
```

### 4. Configure Clients

Players connect via browser console:

```javascript
// In browser console (F12)
const bridge = new WebSocketBridge("ws://localhost:8080");
bridge.connect();
```

Or modify bootstrap to auto-connect:

```javascript
// In bootstrap.js
window.webmcServerUrl = "ws://localhost:8080";
```

---

## WebSocket Bridge Setup

For connecting browsers to real Minecraft servers.

### Architecture

```
Browser                           Minecraft Server
   │                                     ▲
   │  WebSocket (JSON)                   │
   │  ←──────────────────────────────    │
   │                                     │
   │                                     │
   │  TCP (Binary MC Protocol)           │
   └────────────────────────────────────►
              beta-proxy-server.js
                   │
                   └── Bridges WebSocket ↔ TCP
```

### 1. Install Dependencies

```bash
# Server already has dependencies from package.json
cd webmc1
npm install
```

### 2. Start Bridge Server

```bash
# Basic usage
node scripts/beta-proxy-server.js

# With Minecraft server
node scripts/beta-proxy-server.js \
    --ws-port 8080 \
    --mc-host localhost \
    --mc-port 25565

# Production with PM2
pm2 start scripts/beta-proxy-server.js \
    --name webmc-bridge \
    -- \
    --ws-port 8080 \
    --mc-host mc.example.com \
    --mc-port 25565 \
    --max-clients 100
```

### 3. Server Options

```bash
node scripts/beta-proxy-server.js [options]

Options:
  --ws-port <port>       WebSocket server port (default: 8080)
  --mc-host <host>       Minecraft server host (default: localhost)
  --mc-port <port>       Minecraft server port (default: 25565)
  --max-clients <n>      Maximum concurrent clients (default: 100)
  --rate-limit <n>       Max messages per second per client (default: 20)
  --heartbeat <ms>       Heartbeat interval in ms (default: 30000)
```

### 4. Verify Bridge

```bash
# Check stats endpoint
curl http://localhost:8080/stats

# Expected response:
# {
#   "motd": "WebMC Proxy",
#   "version": "1.21.8",
#   "maxPlayers": 100,
#   "playerCount": 0,
#   "connectedClients": 0,
#   "activeServers": []
# }
```

---

## Configuration Options

### Server Configuration (server.js)

```javascript
// server/server.js

const CONFIG = {
  port: 8080,                    // WebSocket port
  maxClients: 20,                // Max players
  chatHistorySize: 100,          // Messages to retain
  heartbeatInterval: 30000,      // ms
  connectionTimeout: 60000,      // ms
};

const serverInfo = {
  motd: 'WebMC Server',          // Server name
  version: '1.21.8',             // Game version
  maxPlayers: 20,                // Player cap
};
```

### Bridge Configuration (beta-proxy-server.js)

```javascript
// scripts/beta-proxy-server.js

const CONFIG = {
  wsPort: 8080,                  // WebSocket port
  mcHost: 'localhost',           // MC server IP
  mcPort: 25565,                 // MC server port
  maxClients: 100,               // Max browser clients
  rateLimit: 20,                 // msg/sec per client
  heartbeatInterval: 30000,      // ms
  connectionTimeout: 60000,      // ms
  reconnectDelay: 3000,          // ms
  maxReconnectAttempts: 5,
};
```

### Client Configuration

In the Java client (`WebSocketBridge.java`):

```java
// Create bridge with server URL
WebSocketBridge bridge = new WebSocketBridge("ws://server:8080");

// Configure reconnection
bridge.setReconnectSettings(5, 3000);  // maxAttempts, delayMs

// Configure heartbeat
bridge.setHeartbeatInterval(30000);     // 30 seconds
```

### Runtime Configuration via URL

```bash
# Connect to specific server
http://localhost:8080/?server=ws://game.example.com:8080
```

---

## Client Configuration

### Browser Console Connection

```javascript
// Open browser console (F12)
const bridge = new WebSocketBridge("ws://localhost:8080");

bridge.setListener({
  onConnect() {
    console.log("Connected!");
    bridge.sendPlayerJoin("Steve");
  },
  
  onPlayerJoin(id, name, x, y, z) {
    console.log(`${name} joined at ${x}, ${y}, ${z}`);
  },
  
  onPositionUpdate(id, x, y, z, yaw, pitch) {
    // Update other player position
  },
  
  onChatMessage(sender, content, timestamp) {
    console.log(`[${sender}] ${content}`);
  },
  
  onDisconnect(reason) {
    console.log("Disconnected:", reason);
  },
  
  onError(error) {
    console.error("Error:", error);
  }
});

bridge.connect();
```

### Automatic Connection (Production)

Modify `addons/web/bootstrap.js`:

```javascript
// Add auto-connect functionality
window.webmcAutoConnect = true;
window.webmcServerUrl = "ws://game.example.com:8080";
```

### Position Updates

Send position updates to server:

```javascript
// In game loop or input handler
function sendPosition() {
  bridge.sendPosition(
    playerX,     // x
    playerY,     // y
    playerZ,     // z
    playerYaw,   // yaw
    playerPitch, // pitch
    onGround     // boolean
  );
}
```

### Chat Messages

```javascript
// Send chat
bridge.sendChat("Hello world!");

// Receive chat via onChatMessage callback
```

---

## Troubleshooting

### Connection Issues

#### "WebSocket connection failed"

1. Verify server is running:
   ```bash
   curl http://localhost:8080/health
   ```

2. Check firewall:
   ```bash
   # Linux
   sudo ufw status
   netstat -tlnp | grep 8080
   ```

3. Check WebSocket URL:
   ```javascript
   console.log("Connecting to:", serverUrl);
   ```

#### "Connection closed unexpectedly"

1. Server may be at capacity:
   ```bash
   curl http://localhost:8080/stats
   ```

2. Check server logs for disconnection reason

#### High Latency

1. Check network path:
   ```bash
   ping server.example.com
   traceroute server.example.com
   ```

2. Server may be overloaded:
   ```bash
   curl http://localhost:8080/stats
   ```

### Multiplayer Sync Issues

#### Players not visible

1. Check console for errors
2. Verify position updates are being sent
3. Check server is broadcasting to all clients

#### Position desync

1. Increase update frequency (not recommended above 20/sec)
2. Check network latency
3. Implement client-side interpolation

#### Chat not working

1. Verify chat messages in browser console:
   ```javascript
   // Enable chat debugging
   bridge.debug = true;
   ```

2. Check rate limiting (20 msg/sec max)

### Bridge Server Issues

#### "Cannot connect to MC server"

1. Verify MC server is running:
   ```bash
   nc -zv mc.server.com 25565
   ```

2. Check bridge server logs

3. Verify MC server accepts connections from bridge IP

#### Player kicked immediately

1. MC server may require authentication
2. Check MC server version compatibility
3. Verify MC server is in online/offline mode

### Performance Issues

#### Server crashes

1. Check memory usage:
   ```bash
   htop
   # or
   pm2 monit
   ```

2. Reduce max clients

3. Add more RAM

#### High CPU usage

1. Check for infinite loops in message handling
2. Reduce heartbeat frequency
3. Optimize message parsing

### Debug Mode

Enable debug logging:

```javascript
// In server.js or beta-proxy-server.js
const DEBUG = true;

// Add to message handlers
if (DEBUG) {
  console.log('[DEBUG] Received:', type, message);
}
```

---

## EaglerPorts Architecture Reference

WebMC's multiplayer architecture is inspired by the Eaglercraft/EaglerPorts project, which pioneered browser-based Minecraft multiplayer.

### Core Concepts

| Concept | EaglerPorts | WebMC |
|---------|-------------|-------|
| Transport | WebSocket | WebSocket |
| Protocol | Custom JSON + Binary | Custom JSON + Binary |
| MC Proxy | bridge-server.js | beta-proxy-server.js |
| Client | Eaglercraft client | WebSocketBridge.java |
| Auth | Offline only (typically) | Offline only |

### Protocol Comparison

#### EaglerPorts (Original)

```javascript
// EaglerPorts message format
{
  "2": {           // Packet ID
    "a": "uuid",   // Player ID
    "b": "name",   // Player name
    "c": [x,y,z],  // Position
    "d": yaw,
    "e": pitch
  }
}
```

#### WebMC (Current)

```javascript
// WebMC message format
{
  "type": "position",
  "playerId": "uuid",
  "x": 0, "y": 64, "z": 0,
  "yaw": 0, "pitch": 0,
  "onGround": true
}
```

### Key Differences

1. **JSON Structure**: EaglerPorts uses numeric packet IDs, WebMC uses string types
2. **Binary Protocol**: Both support binary for MC packet forwarding
3. **Bridge Mode**: EaglerPorts uses `vprox`, WebMC uses `beta-proxy-server.js`
4. **Client Integration**: EaglerPorts is a fork, WebMC integrates via TeaVM

### Migration Notes

If migrating from EaglerPorts:

1. Replace `vprox` with `beta-proxy-server.js`
2. Update client message handlers
3. Use `WebSocketBridge.java` instead of Eagler client
4. Maintain same WebSocket endpoint format

### Security Considerations

Both architectures are designed for local/LAN play. For production:

1. **Authentication**: Add session tokens
2. **Rate Limiting**: Already implemented
3. **Encryption**: Use WSS (WebSocket Secure)
4. **Anti-Cheat**: Add server-side validation
5. **Moderation**: Implement chat filtering

### Future Enhancements

Planned improvements for WebMC multiplayer:

- [ ] Authentication system
- [ ] Server listing API
- [ ] UUID-based player persistence
- [ ] Permission system
- [ ] Anti-cheat measures
- [ ] Voice chat integration
- [ ] Plugin API

---

## Appendix: API Reference

### Server Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Server health status |
| `/stats` | GET | Server statistics |

### WebSocket Message Types

#### Client to Server

| Type | Fields | Description |
|------|--------|-------------|
| `ping` | `timestamp` | Heartbeat request |
| `player_join` | `name` | Join game |
| `position` | `x,y,z,yaw,pitch,onGround` | Position update |
| `chat` | `content` | Chat message |
| `mc_connect` | `host,port` | Connect to MC server |
| `mc_packet` | `data` (base64) | Raw MC packet |
| `server_info_request` | - | Request server info |

#### Server to Client

| Type | Fields | Description |
|------|--------|-------------|
| `pong` | `timestamp` | Heartbeat response |
| `player_join` | `playerId,name,x,y,z` | Player joined |
| `player_leave` | `playerId,name` | Player left |
| `position` | `playerId,x,y,z,yaw,pitch` | Position update |
| `chat` | `sender,content,timestamp` | Chat message |
| `system` | `content,timestamp` | System message |
| `server_info` | `motd,version,playerCount,maxPlayers` | Server info |
| `player_list` | `players[]` | All players |
| `mc_connected` | `host,port` | MC server connected |
| `mc_packet` | `data` (base64) | Raw MC packet |
| `error` | `message` | Error message |

### Connection State Machine

```
┌──────────────┐
│ DISCONNECTED │ ←── Initial state
└──────┬───────┘
       │ connect()
       ▼
┌──────────────┐
│ CONNECTING   │ ←── Attempting connection
└──────┬───────┘
       │ onOpen()
       ▼
┌──────────────┐
│ CONNECTED    │ ←── WebSocket open
└──────┬───────┘
       │ sendPlayerJoin()
       ▼
┌────────────────┐
│ AUTHENTICATED  │ ←── Player registered
└──────┬─────────┘
       │
       ├──── mc_connect() ──→ BRIDGED (MC server)
       │
       └──── (local play) ──→ stays AUTHENTICATED
```

---

## Appendix: Example Configurations

### LAN Party Config (5-10 players)

```bash
node server/server.js 8080
```

```javascript
// No special client config needed
// All players on same network
const bridge = new WebSocketBridge("ws://192.168.1.100:8080");
```

### Small Server Config (20-50 players)

```bash
# server with custom settings
cat > server-config.js << 'EOF'
module.exports = {
  port: 8080,
  maxClients: 50,
  chatHistorySize: 200,
  serverInfo: {
    motd: 'My WebMC Server',
    version: '1.21.8',
    maxPlayers: 50
  }
};
EOF

PORT=8080 MAX_CLIENTS=50 node server/server.js
```

### Production Config (100+ players)

```bash
# Using PM2 process manager
pm2 start scripts/beta-proxy-server.js \
    --name webmc-prod \
    -- \
    --ws-port 8080 \
    --mc-host mc.example.com \
    --mc-port 25565 \
    --max-clients 200 \
    --rate-limit 30 \
    --heartbeat 60000

# With nginx reverse proxy
pm2 save
pm2 startup
```