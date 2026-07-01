# WebMC System Architecture

This document provides a comprehensive overview of the WebMC system architecture, covering the TeaVM compilation pipeline, VFS file format, WebGL rendering, multiplayer networking, and build system.

## Table of Contents

1. [Overall Architecture](#overall-architecture)
2. [TeaVM Compilation Pipeline](#teavm-compilation-pipeline)
3. [VFS File Format](#vfs-file-format)
4. [WebGL Rendering Flow](#webgl-rendering-flow)
5. [Multiplayer Architecture](#multiplayer-architecture)
6. [Build System Overview](#build-system-overview)

---

## Overall Architecture

```
+------------------------------------------------------------------+
|                         WebMC Architecture                         |
+------------------------------------------------------------------+

  +------------------+     +------------------+     +------------------+
  |   Minecraft      | --> |   WebMC Build    | --> |   Browser        |
  |   1.21.8 Source  |     |   System         |     |   Client         |
  +------------------+     +------------------+     +------------------+
        |                        |                        |
        v                        v                        v
  +------------------+     +------------------+     +------------------+
  |  Java 21 Code    |     |  TeaVM Compiler  |     |  JavaScript      |
  |  (Gradle + MCP)  |     |  (Java -> JS)    |     |  (game.js ~290MB)|
  +------------------+     +------------------+     +------------------+
                                  |
                                  v
                          +------------------+
                          |  VFS Generator   |
                          |  (Resource Pack) |
                          +------------------+
                                  |
                                  v
                          +------------------+
                          |  game.vfs        |
                          |  (~895MB)        |
                          +------------------+

  +------------------------------------------------------------------+
  |                         Browser Runtime                           |
  +------------------------------------------------------------------+
  
  +---------------+   +---------------+   +---------------+   +--------+
  |  bootstrap.js |-->|  game.js      |-->|  WebGL2       |-->|  VFS   |
  |  (UI/Input)   |   |  (TeaVM)      |   |  (Canvas)     |   |  Cache |
  +---------------+   +---------------+   +---------------+   +--------+
         |                  |                   |                   |
         v                  v                   v                   v
  +---------------+   +---------------+   +---------------+   +--------+
  |  Console      |   |  MC Game      |   |  Render       |   |IndexedDB|
  |  Filter       |   |  Loop         |   |  Pipeline     |   |        |
  +---------------+   +---------------+   +---------------+   +--------+
```

---

## TeaVM Compilation Pipeline

### Overview

WebMC uses [TeaVM](https://teavm.org/) to compile Minecraft Java code into JavaScript for browser execution. This is a critical transformation that enables running Minecraft in a web browser without plugins.

### Build Configuration

Located in `work/build.gradle`:

```gradle
teavm {
    all {
        mainClass = 'top.steve3184.webmc.web.WebMain'
        outOfProcess = true
        processMemory = 8192  // 8GB heap for compilation
    }
    js {
        targetFileName = 'game.js'
        sourceMap = false
        optimization = org.teavm.gradle.api.OptimizationLevel.NONE
        fastGlobalAnalysis = true
        obfuscated = false
        strict = false
        moduleType = org.teavm.gradle.api.JSModuleType.NONE
    }
}
```

### Key TeaVM Features Used

| Feature | Purpose |
|---------|---------|
| `moduleType = NONE` | Produces IIFE format for classic `<script>` inclusion |
| `outOfProcess = true` | Runs TeaVM in separate process for better memory management |
| `fastGlobalAnalysis` | Faster but less aggressive dead code elimination |
| `mainClass` | Entry point class for browser execution |

### Compilation Stages

```
1. Java Compilation
   ├── Standard Gradle Java compilation
   ├── JDK Stubs compilation (locks, sun.misc, jdk.unsupported)
   └── TeaVM plugin processing

2. TeaVM Analysis
   ├── Bytecode analysis
   ├── Type inference
   ├── Method dependency graph
   └── Dead code elimination

3. JavaScript Generation
   ├── IIFE wrapper generation
   ├── Function hoisting
   ├── String pool creation
   └── Runtime library inclusion

4. Output
   └── game.js (~290MB uncompressed, ~19MB Brotli)
```

### Bootstrap Integration

The `bootstrap.js` file loads the compiled JavaScript:

```javascript
// TeaVM with moduleType=NONE produces an IIFE
// that registers window.main as the entry point
function runTeaVmMain() {
    if (typeof window.main !== 'function') {
        fatal('game.js did not register window.main');
        return;
    }
    // TeaVM main signature: main(args: string[], callback?: () => void)
    window.main([], function() {
        // Game initialized callback
    });
}
```

### Known Issues and Workarounds

1. **Obfuscation Bug**: TeaVM's AGGRESSIVE obfuscation causes variable name conflicts in mouse event handlers. See `scripts/fix-teavm-bug.js`.

2. **Let Keyword Bug**: The `let` keyword causes issues in some environments. See `scripts/fix-teavm-let-bug.js`.

3. **Comma Operator Bug**: Trailing comma parsing issues. See `scripts/fix-teavm-comma-bug.js`.

---

## VFS File Format

### Overview

The Virtual File System (VFS) is a custom archive format containing all Minecraft assets (textures, sounds, models, etc.) compiled for browser delivery.

### File Structure

```
game.vfs
├── Header (16 bytes)
│   ├── Magic: "VFS\0" (4 bytes)
│   ├── Version: uint32 (4 bytes)
│   ├── Entry count: uint32 (4 bytes)
│   └── Reserved: uint32 (4 bytes)
│
├── Index Section
│   ├── Entry[0]: path_length, path, offset, size
│   ├── Entry[1]: path_length, path, offset, size
│   └── ... (N entries)
│
├── Data Section
│   ├── Entry[0] data (offset from header)
│   ├── Entry[1] data
│   └── ... (interleaved or sequential)
│
└── End of file
```

### VFS Generation

The VFS is generated during the build process from the Minecraft resource pack:

1. Extract all assets from Minecraft JAR
2. Process and optimize (TinyPNG compression for textures)
3. Build index structure
4. Package into `game.vfs`

### VFS Caching System

WebMC implements a sophisticated caching layer:

```
Browser Request
      │
      ▼
┌─────────────────┐
│  VfsLoader      │  Checks IndexedDB cache first
│  (vfs-loader.js)│  Downloads .xz compressed VFS
└────────┬────────┘  Decompresses with WASM
         │
         ▼
┌─────────────────┐
│  VfsCache       │  IndexedDB wrapper
│  (vfs-cache.js) │  Version-based invalidation
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  IndexedDB      │  Persistent browser storage
│  (webmc_vfs DB) │  ~895MB capacity
└─────────────────┘
```

### Cache API

```javascript
// Preload VFS before game starts
await VfsLoader.preloadToWebFs('game.vfs', {
    version: '1.21.8',
    onProgress: (percent, status) => {
        setBootStatus(status, percent);
    }
});

// Debug: Clear cache
await VfsLoader.clearCache();

// Debug: Get storage info
const info = await VfsLoader.getStorageInfo();
// { usage: 938862080, quota: 2147483648, percent: 43 }
```

### Loading Pipeline

```
index.html
    │
    ├── vfs-cache.js      (IndexedDB wrapper)
    │
    ├── vfs-loader.js     (Download + decompress)
    │
    └── bootstrap.js      (Game bootstrap)
            │
            ├── preloadVfs()  → VfsLoader.preloadToWebFs()
            │
            ├── loadGameScript()  → fetch('game.js')
            │
            └── runTeaVmMain()    → window.main()
```

---

## WebGL Rendering Flow

### Architecture

WebMC uses WebGL2 for hardware-accelerated 3D rendering. The rendering pipeline is implemented in Java and compiled to JavaScript via TeaVM.

### Canvas Setup

```javascript
// index.html
<canvas id="canvas" width="1280" height="720" tabindex="0"></canvas>

// bootstrap.js
const $canvas = document.getElementById('canvas');
```

### Input Handling

The `InputBridge` class bridges DOM events to LWJGL-style callbacks:

```
DOM Event          │  InputBridge (JS)         │  LWJGL Callback
───────────────────┼──────────────────────────┼────────────────────
keydown            │  queueKeyEvent()          │  GLFWKeyCallback
keyup              │  queueKeyEvent()          │  GLFWKeyCallback
keypress           │  queueCharEvent()         │  GLFWCharCallback
mousemove          │  queueCursorPosEvent()    │  GLFWCursorPosCallback
mousedown          │  queueMouseButtonEvent()  │  GLFWMouseButtonCallback
mouseup            │  queueMouseButtonEvent()  │  GLFWMouseButtonCallback
wheel              │  queueScrollEvent()       │  GLFWScrollCallback
focus/blur         │  queueFocusEvent()        │  GLFWWindowFocusCallback
resize             │  queueFramebufferSizeEvent│  GLFWFramebufferSizeCallback
```

Architecture:
```
JavaScript DOM Events
        │
        ▼
┌───────────────────┐
│  InputBridge.js   │  (bootstrap.js - setupInputBridge)
│  queue*Event()    │
└─────────┬─────────┘
          │ @Export methods
          ▼
┌───────────────────┐
│  InputBridge.java │  (teavm/input/InputBridge.java)
│  Event Queue      │
└─────────┬─────────┘
          │ pollEvents()
          ▼
┌───────────────────┐
│  GLFW Callbacks   │  (teavm/callback/*.java)
└───────────────────┘
```

### Pointer Lock

For in-game camera control:

```javascript
// Request pointer lock when entering game
if (document.pointerLockElement !== canvas) {
    canvas.requestPointerLock();
}

// Handle pointer lock state changes
document.addEventListener('pointerlockchange', () => {
    if (document.pointerLockElement === canvas) {
        hidePointerHint();  // Game has mouse control
    } else {
        showPointerHint();  // Show "Click to lock" message
    }
});
```

### Render Pipeline

```
Game Loop (60 FPS target)
    │
    ├── Input Processing
    │   ├── Keyboard state
    │   ├── Mouse position
    │   └── Mouse buttons
    │
    ├── Game Logic
    │   ├── Entity updates
    │   ├── Physics simulation
    │   └── World tick
    │
    ├── Chunk Compilation
    │   ├── Visibility culling
    │   ├── Mesh generation
    │   └── Buffer uploads
    │
    ├── Render Pass
    │   ├── Clear buffers
    │   ├── Setup matrices
    │   ├── Draw chunks
    │   ├── Draw entities
    │   └── Post-processing
    │
    └── Present
        └── requestAnimationFrame()
```

### Chunk Rendering

WebMC uses chunk-based terrain rendering:

1. **Chunk Size**: 16x16x(world height) blocks
2. **Visible Range**: Configurable render distance
3. **Mesh Compilation**: Greedy meshing for optimization
4. **Section Dispatch**: Parallel chunk processing

### State Beacon System

The bootstrap monitors game state via a beacon URL:

```javascript
// Java sets state beacon
window.__webmcState = {
    visibleSections: 150,
    renderedSections: 120,
    requiredRenderedSections: 200,
    levelPresent: true,
    playerPresent: true,
    screen: 'GameScreen'
};

// Bootstrap polls via Image.src hijacking
function inspectStateUrl(value) {
    if (url.pathname === '/__webmc_state') {
        handleWebMcState(source, JSON.parse(params.get('d')));
        return true;  // Suppress actual image load
    }
}
```

---

## Multiplayer Architecture

### Overview

WebMC supports multiplayer through a WebSocket-based bridge architecture inspired by the EaglerPorts/Eaglercraft model.

### Architecture Diagram

```
+------------------------------------------------------------------+
|                     WebMC Multiplayer Architecture                |
+------------------------------------------------------------------+

  Browser 1              Browser 2              Browser N
     │                       │                       │
     v                       v                       v
+----------+           +----------+           +----------+
| WebSocket|           | WebSocket|           | WebSocket|
| Client   |           | Client   |           | Client   |
+----------+           +----------+           +----------+
     │                       │                       │
     └───────────────────────┼───────────────────────┘
                             │
                             v
              +----------------------------+
              |   beta-proxy-server.js     |
              |   (WebSocket Bridge)       |
              +----------------------------+
              │                            │
              ├── Local Mode (LAN Play)    │
              │   └── No MC server needed  │
              │       Player sync only     │
              │                            │
              └── Bridge Mode (Real MC)    │
                  └── TCP to MC Server     │
                      (port 25565)         │
                                         
  Java Client (WebSocketBridge.java)
        │
        ├── player_join
        ├── position updates
        ├── chat messages
        └── mc_packet (binary)
```

### WebSocket Protocol

#### Message Format

All messages are JSON with a `type` field:

```javascript
// Client → Server
{ "type": "player_join", "name": "Steve" }
{ "type": "position", "x": 0, "y": 64, "z": 0, "yaw": 0, "pitch": 0, "onGround": true }
{ "type": "chat", "content": "Hello world!" }
{ "type": "mc_connect", "host": "mc.server.com", "port": 25565 }

// Server → Client
{ "type": "player_join", "playerId": "uuid", "name": "Steve", "x": 0, "y": 64, "z": 0 }
{ "type": "player_leave", "playerId": "uuid", "name": "Steve" }
{ "type": "position", "playerId": "uuid", "x": 0, "y": 64, "z": 0, "yaw": 0, "pitch": 0 }
{ "type": "chat", "sender": "Steve", "content": "Hello!", "timestamp": 1234567890 }
{ "type": "server_info", "motd": "WebMC Server", "version": "1.21.8", "playerCount": 5, "maxPlayers": 20 }
```

#### Connection States

```
DISCONNECTED → CONNECTING → CONNECTED → AUTHENTICATED
                                    ↓
                                RECONNECTING (on disconnect)
                                    ↓
                                   FAILED (max retries exceeded)
```

### Bridge Server Features

| Feature | Description |
|---------|-------------|
| Local Mode | Browser-to-browser player sync without MC server |
| Bridge Mode | Connect to real Minecraft servers via TCP |
| Rate Limiting | 20 msg/sec per client (configurable) |
| Heartbeat | 30-second ping/pong for connection health |
| Chat History | Last 100 messages stored and delivered to new players |
| Health Endpoints | `/health` and `/stats` HTTP endpoints |

### Java Client Integration

```java
// WebSocketBridge.java
WebSocketBridge bridge = new WebSocketBridge("ws://localhost:8080");
bridge.setListener(new BridgeListener() {
    @Override
    public void onConnect() {
        bridge.sendPlayerJoin("Steve");
    }
    
    @Override
    public void onPlayerJoin(String playerId, String name, double x, double y, double z) {
        // Spawn other player
    }
    
    @Override
    public void onPositionUpdate(String playerId, double x, double y, double z, float yaw, float pitch) {
        // Update other player position
    }
    
    @Override
    public void onChatMessage(String sender, String content, long timestamp) {
        // Display chat message
    }
});
bridge.connect();
```

### Server Implementation

Key files:
- `server/server.js` - Simple WebSocket server for local play
- `scripts/beta-proxy-server.js` - Full-featured bridge with MC server support

---

## Build System Overview

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Build Tool | Gradle | 8.x |
| Language | Java | 21 |
| Mod Loader | ForgeGradle | 6+ |
| Mappings | Mojang (official) | 1.21.8 |
| Java→JS | TeaVM | 0.13.1 |
| JS Bundling | None (raw IIFE) | - |
| Compression | Brotli | via `brotli` CLI |

### Project Structure

```
webmc1/
├── work/                          # Main workspace
│   ├── build.gradle               # Gradle build configuration
│   ├── build/                     # Build output
│   │   ├── generated/teavm/js/    # TeaVM output (game.js)
│   │   ├── generated/teavm/vfs/   # VFS output (game.vfs)
│   │   └── web-run/               # Final distribution
│   │       ├── index.html
│   │       ├── bootstrap.js
│   │       ├── game.js
│   │       ├── game.js.br         # Brotli compressed
│   │       ├── game.vfs
│   │       ├── game.vfs.br
│   │       └── chunks/            # Code-split chunks
│   └── src/main/java/             # Custom Java code
│       └── top/steve3184/webmc/
│           ├── net/               # Networking
│           │   └── WebSocketBridge.java
│           └── teavm/runtime/     # TeaVM runtime
│               └── CanvasWindowBackend.java
├── server/                        # Multiplayer server
│   ├── server.js                  # Simple WebSocket server
│   └── README.md
├── scripts/                       # Build utilities
│   ├── serve.js                   # Development server
│   ├── beta-proxy-server.js       # MC bridge server
│   ├── split-game-js.mjs          # Code splitting tool
│   └── fix-*.js                   # TeaVM bug workarounds
├── addons/                        # Web resources
│   └── web/
│       ├── index.html
│       └── bootstrap.js
├── docs/                          # This documentation
├── package.json                   # Node.js dependencies
└── upstream/                      # Minecraft source
    └── projects/mcp/              # MCP configured source
```

### Build Tasks

```bash
# Full web build (includes compression)
./gradlew buildWebRun

# Or with individual tasks:
./gradlew generateJavaScript    # TeaVM compilation
./gradlew assembleWebRun        # Copy resources
./gradlew compressWebAssets     # Brotli compression

# Development
./gradlew setup                 # Download assets
./gradlew runclient             # Run Minecraft client
./gradlew runserver             # Run Minecraft server

# Testing
./gradlew runtimeCheckMcMainPhase197  # Playwright test
```

### Build Output

| File | Uncompressed | Brotli | Notes |
|------|-------------|--------|-------|
| game.js | ~290 MB | ~19 MB | TeaVM output |
| game.vfs | ~895 MB | ~691 MB | Assets |
| bootstrap.js | ~27 KB | ~5.6 KB | Bootstrap |
| index.html | ~6 KB | ~1.5 KB | Entry point |

### Compression Pipeline

```javascript
// compressWebAssets task in build.gradle
filesToCompress.each { fname ->
    def source = new File(webRunDir, fname)
    def target = new File(webRunDir, fname + ".br")
    
    // Uses system `brotli` CLI
    def process = "brotli -f -o ${target} ${source}".execute()
    process.waitFor()
    
    // Result: game.js 290MB → 19MB (93% reduction)
}
```

### Development Server

```javascript
// scripts/serve.js
// Supports:
// - Static file serving
// - Brotli (.br) serving when client supports it
// - Gzip (.gz) fallback

// Usage:
node scripts/serve.js 8080

// Endpoints:
// GET /           → index.html
// GET /game.js    → game.js or game.js.br (if accepted)
// GET /game.vfs   → game.vfs or game.vfs.br (if accepted)
```

---

## Appendix: Key Classes

### Java (compiled to JS)

| Class | Purpose |
|-------|---------|
| `top.steve3184.webmc.web.WebMain` | TeaVM entry point |
| `CanvasWindowBackend` | DOM to LWJGL bridge |
| `WebSocketBridge` | Multiplayer networking |

### JavaScript (runtime)

| File | Purpose |
|------|---------|
| `bootstrap.js` | UI, loading, game initialization |
| `vfs-loader.js` | Asset downloading and caching |
| `vfs-cache.js` | IndexedDB wrapper |
| `server.js` | Multiplayer WebSocket server |

---

## Appendix: Console Filter

WebMC filters noisy console output by default:

```javascript
const noisyPrefixes = [
    '[mc-web/serverchunks]',
    '[mc-web/chunks]',
    '[mc-web/sectionCompile]',
    '[mc-web/renderLevel]',
    '[mc-web/clienttick]',
    // ... 80+ prefixes
];

console.log = (...args) => {
    const text = String(args[0]);
    if (noisyPrefixes.some(p => text.startsWith(p))) {
        return;  // Suppress
    }
    originalLog(...args);
};
```

Enable diagnostics mode to see all output:
```
index.html?diagnostics=1
```