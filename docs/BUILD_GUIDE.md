# WebMC Build and Deployment Guide

This guide covers prerequisites, build commands, development workflow, testing, and deployment for WebMC.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Build Commands](#build-commands)
4. [Development Workflow](#development-workflow)
5. [Testing](#testing)
6. [Deployment](#deployment)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Component | Minimum Version | Recommended | Purpose |
|-----------|-----------------|-------------|---------|
| Java (JDK) | 21 | 21 LTS | Build and runtime |
| Node.js | 18 | 20 LTS | Server, tooling |
| Gradle | 8.x | Bundled | Build system |
| Brotli | Any | Latest | Asset compression |

### Verify Installations

```bash
# Check Java version
java -version
# Expected: openjdk version "21.x.x" or similar

# Check Node.js version
node --version
# Expected: v18.x.x or higher

# Check npm
npm --version
# Expected: 9.x.x or higher

# Check Brotli (optional, for compression)
brotli --version
# If not found, compression will be skipped
```

### System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 8 GB | 16 GB |
| Disk Space | 10 GB | 20 GB |
| CPU | 4 cores | 8+ cores |

**Note**: TeaVM compilation requires significant memory (8GB heap configured).

---

## Initial Setup

### 1. Clone and Navigate

```bash
git clone https://cnb.cool/star-mc.top/webmc1.git
cd webmc1
```

### 2. Install Node.js Dependencies

```bash
npm install
```

This installs:
- `acorn` - JavaScript parser for code splitting
- `escodegen` - Code generator
- `playwright` - Browser testing
- `wabt` - WebAssembly toolkit

### 3. Download Minecraft Assets

```bash
cd work
./gradlew setup
```

This downloads:
- Minecraft client JAR
- Server JAR
- Asset index and resources
- LWJGL natives

**Time**: 5-15 minutes depending on connection

### 4. Build the Project

```bash
# Full build (includes VFS generation)
./gradlew buildWebRun
```

**Time**: 15-30 minutes on first build

---

## Build Commands

### Standard Build Tasks

```bash
# Navigate to work directory
cd work

# ============================================
# FULL BUILD PIPELINE
# ============================================

# Build everything (recommended)
./gradlew buildWebRun

# Equivalent to:
./gradlew generateJavaScript assembleWebRun compressWebAssets
```

### Individual Tasks

```bash
# ============================================
# TEAVM COMPILATION
# ============================================

# Compile Java to JavaScript (longest step)
./gradlew generateJavaScript
# Output: build/generated/teavm/js/game.js

# ============================================
# VFS GENERATION
# ============================================

# Generate VFS archive
./gradlew buildVfs
# Output: build/generated/teavm/vfs/game.vfs

# ============================================
# ASSEMBLY
# ============================================

# Copy all web assets to web-run/
./gradlew assembleWebRun
./gradlew assembleWebRunFinal
# Output: build/web-run/

# ============================================
# COMPRESSION
# ============================================

# Brotli compress assets (requires 'brotli' CLI)
./gradlew compressWebAssets
# Creates: game.js.br, game.vfs.br, etc.
```

### Development Tasks

```bash
# Download and setup assets
./gradlew setup
./gradlew copyAssets

# Run Minecraft client (for debugging Java side)
./gradlew runclient

# Run Minecraft server (for testing multiplayer)
./gradlew runserver

# Clean build artifacts
./gradlew clean
```

### Code Splitting (Optional)

```bash
# Split large game.js into chunks
node scripts/smart-split.mjs \
    work/build/generated/teavm/js/game.js \
    work/build/generated/teavm/js/chunks

# Or use the create-loader utility
node scripts/create-loader.mjs \
    work/build/generated/teavm/js/chunks
```

### Gradle Options

```bash
# Parallel builds (faster on multi-core)
./gradlew buildWebRun --parallel

# Verbose output
./gradlew buildWebRun --info

# Skip TeaVM (use cached output)
./gradlew buildWebRun -x generateJavaScript

# Specify memory for Gradle
./gradlew buildWebRun -Dorg.gradle.jvmargs="-Xmx8g"

# Continue on errors
./gradlew buildWebRun --continue
```

---

## Development Workflow

### Development Server

Start a local development server:

```bash
# From project root
node scripts/serve.js 8080

# Or from web-run directory directly
cd work/build/web-run
npx serve .

# Or use Python (no dependencies)
cd work/build/web-run
python -m http.server 8080
```

The server supports:
- Automatic Brotli compression detection
- Brotli (.br) file serving when client supports it
- Gzip (.gz) fallback

### Hot Reload Development

WebMC does not support true hot reload (JavaScript is pre-compiled). For changes:

1. **Java/TeaVM changes**: Rebuild with `./gradlew generateJavaScript`
2. **Bootstrap/HTML changes**: Just refresh browser
3. **VFS changes**: Rebuild with `./gradlew buildVfs`

### Debug Mode

Enable diagnostic output:

```bash
# Via URL parameter
http://localhost:8080/?diagnostics=1

# Via JavaScript console
window.webmcDiagnostics = true;
location.reload();
```

### Auto-start World

For faster testing:

```bash
# Via URL parameter
http://localhost:8080/?autoStartExperimentalWorld=TestWorld

# Or via bootstrap config
window.webmcAutoStartExperimentalWorld = 'TestWorld';
```

### Boot Modes

```bash
# Default: Full Minecraft gameplay
http://localhost:8080/?boot=mcMain

# Minimal: Skip MC Main.main, useful for testing bootstrap
http://localhost:8080/?boot=webSafeBoot
```

### File Sizes

Monitor build output sizes:

```bash
ls -lh work/build/web-run/
```

Expected sizes:
```
game.js         ~290 MB  (source)
game.js.br      ~19 MB   (Brotli compressed, 93% reduction)
game.vfs        ~895 MB  (source)
game.vfs.br     ~691 MB  (Brotli compressed, 23% reduction)
```

---

## Testing

### Automated Testing (Playwright)

Run the phase 197 test to verify game loads:

```bash
# From project root
npm test

# Or explicitly
node scripts/phase197-test.js
```

The test:
1. Starts a local server
2. Opens browser with Playwright
3. Navigates to WebMC
4. Waits for game to reach main menu
5. Verifies no critical errors
6. Takes screenshot on failure

### Manual Testing Checklist

#### Loading
- [ ] Game loads without errors
- [ ] Progress bar shows during loading
- [ ] Main menu appears after loading
- [ ] No console errors (check F12)

#### Gameplay
- [ ] Click "Singleplayer" to start world
- [ ] Terrain renders correctly
- [ ] Player can move (WASD)
- [ ] Player can look around (mouse)
- [ ] Pointer lock works in-game

#### UI
- [ ] Pause menu works (Esc)
- [ ] Chat input works (T key)
- [ ] Inventory opens (E key)
- [ ] Settings accessible

#### Multiplayer
- [ ] Connect to local server
- [ ] See other players
- [ ] Position sync works
- [ ] Chat messages appear

### Browser Compatibility

| Browser | Support | Notes |
|---------|---------|-------|
| Chrome 90+ | Full | Recommended |
| Firefox 90+ | Full | Full support |
| Safari 15+ | Partial | Some WebGL issues possible |
| Edge 90+ | Full | Chromium-based |
| Mobile | Not supported | Not a target |

### Performance Benchmarks

Typical loading times (with Brotli):
- Initial load: 10-30 seconds
- Cache hit: 2-5 seconds
- VFS size: ~691 MB compressed

Memory usage:
- Initial: ~200-300 MB
- Peak: ~500-800 MB
- After world load: ~400-600 MB

---

## Deployment

### Static Hosting Requirements

WebMC requires:
- HTTP server with:
  - Brotli (`br`) encoding support
  - Large file support (>1GB)
  - WebSocket support (for multiplayer)
- HTTPS (required for some browser APIs)

### Recommended Hosting Options

| Provider | Suitability | Notes |
|----------|-------------|-------|
| VPS (DigitalOcean, Vultr) | Best | Full control, WebSocket support |
| Cloudflare Pages | Good | Free, Brotli, needs R2 for large files |
| AWS S3 + CloudFront | Good | Expensive for large files |
| GitHub Pages | Limited | 1GB limit, no WebSocket |
| Netlify | Limited | 250MB limit, no WebSocket |

### Deployment Steps

#### 1. Build the Project

```bash
cd work
./gradlew buildWebRun
```

#### 2. Prepare Deployment Files

```bash
cd work/build/web-run

# Verify files exist
ls -la *.br *.js index.html
```

#### 3. Deploy to Server

```bash
# Option A: Rsync to VPS
rsync -avz --progress \
    build/web-run/ \
    user@server:/var/www/webmc/

# Option B: Docker deployment
docker build -t webmc .
docker run -p 8080:8080 -p 8081:8081 webmc
```

#### 4. Configure Server

Ensure server supports:
- Brotli encoding
- WebSocket upgrades
- Large file downloads

Example nginx config:

```nginx
server {
    listen 80;
    server_name webmc.example.com;
    
    root /var/www/webmc;
    index index.html;
    
    # Enable Brotli
    load_module modules/ngx_http_brotli_filter_module.so;
    
    brotli on;
    brotli_types text/html text/css application/javascript;
    brotli_comp_level 6;
    
    # Large file support
    client_max_body_size 2G;
    
    location / {
        try_files $uri $uri/ =404;
    }
    
    # WebSocket for multiplayer
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### Multiplayer Server Deployment

Deploy the WebSocket server:

```bash
# SSH to server
ssh user@server

# Clone or update repo
git pull

# Install Node dependencies
cd server
npm install

# Start server (production)
pm2 start server.js --name webmc-server

# Or with custom port
pm2 start server.js --name webmc-server -- 3000

# With beta-proxy (MC bridge mode)
pm2 start ../scripts/beta-proxy-server.js \
    --name webmc-bridge \
    -- --ws-port 8080 --mc-host mc.server.com --mc-port 25565
```

### CDN Considerations

For large VFS files, consider:
- CDN for game.js (frequently cached)
- Direct origin for game.vfs (less cacheable)
- Signed URLs for private deployments

### SSL/TLS

Required for:
- Service Worker registration
- Some browser APIs
- Better browser performance

```bash
# Let's Encrypt (recommended)
certbot --nginx -d webmc.example.com

# Or Cloudflare (automatic)
# Just enable CDN proxy
```

---

## Troubleshooting

### Common Build Issues

#### Out of Memory

```
Error: Java heap space
```

Solution:
```bash
./gradlew generateJavaScript -Dorg.gradle.jvmargs="-Xmx12g"
```

#### TeaVM Compilation Fails

```
Error: Cannot find main class: top.steve3184.webmc.web.WebMain
```

Solution:
```bash
# Ensure Java code compiles first
./gradlew compileJava
./gradlew generateJavaScript
```

#### Brotli Not Found

```
Error: brotli command not found
```

Solution:
```bash
# macOS
brew install brotli

# Ubuntu/Debian
apt install brotli

# Windows (use WSL or download binary)
```

#### Missing Dependencies

```
Error: Cannot find module 'acorn'
```

Solution:
```bash
npm install
```

### Runtime Issues

#### Game Won't Load

1. Check browser console (F12) for errors
2. Enable diagnostics: `?diagnostics=1`
3. Clear VFS cache:
   ```javascript
   webmc.clearVfsCache()
   ```

#### Black Screen

1. Check WebGL support: https://get.webgl.org/
2. Try different browser
3. Check console for shader compilation errors

#### Performance Issues

1. Reduce render distance in Minecraft settings
2. Close other browser tabs
3. Use hardware-accelerated GPU:
   ```
   chrome://settings/system
   ```

#### Multiplayer Not Working

1. Verify server is running: `curl http://localhost:8080/health`
2. Check browser console for WebSocket errors
3. Verify firewall allows WebSocket connections

### Gradle Issues

#### Stuck Downloads

```bash
# Clean Gradle cache
./gradlew --stop
rm -rf ~/.gradle/caches
./gradlew buildWebRun --refresh-dependencies
```

#### Wrong Java Version

```bash
# Check Java version
java -version

# Use Java 21 specifically
export JAVA_HOME=/path/to/jdk-21
./gradlew buildWebRun
```

### Getting Help

1. Check existing GitHub issues
2. Enable diagnostics and capture console output
3. Include:
   - Browser version
   - Java version
   - Gradle output
   - Console errors
   - Screenshot if applicable

---

## Appendix: Build Commands Reference

```bash
# Quick reference
cd work

# Build
./gradlew buildWebRun           # Full build
./gradlew clean                 # Clean

# Development
./gradlew setup                 # Download assets
./gradlew runclient             # Run MC client

# Testing
./gradlew runtimeCheckMcMainPhase197  # Playwright test

# Server
cd ../server
npm start                       # Start multiplayer server
node ../scripts/beta-proxy-server.js  # MC bridge server

# Serve locally
cd ../scripts
node serve.js 8080
```