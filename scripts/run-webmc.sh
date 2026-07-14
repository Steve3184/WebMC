#!/bin/bash
# WebMC Development Server Launcher

set -e

WEB_RUN_DIR="build/web-run"

echo "========================================"
echo "  WebMC Development Server"
echo "========================================"

# Check if TeaVM output exists
if [ ! -f "$WEB_RUN_DIR/game.js" ]; then
    echo "ERROR: TeaVM output not found at $WEB_RUN_DIR/game.js"
    echo "Please run './gradlew generateJavaScript' first"
    exit 1
fi

# Copy HTML and JS files to web-run
echo "Setting up web files..."

# Copy index.html
if [ -f "addons/web/index.html" ]; then
    cp "addons/web/index.html" "$WEB_RUN_DIR/"
    echo "  - Copied index.html"
fi

# Copy required JS files
for js in vfs.js socket.js performance.js performance-hud.js; do
    if [ -f "addons/web/$js" ]; then
        cp "addons/web/$js" "$WEB_RUN_DIR/"
        echo "  - Copied $js"
    else
        echo "  - WARNING: $js not found"
    fi
done

# Copy bootstrap.js (required)
if [ -f "addons/web/bootstrap.js" ]; then
    cp "addons/web/bootstrap.js" "$WEB_RUN_DIR/"
    echo "  - Copied bootstrap.js"
else
    echo "ERROR: bootstrap.js not found"
    exit 1
fi

echo ""
echo "Files in $WEB_RUN_DIR:"
ls -la "$WEB_RUN_DIR/" | grep -E "\.(js|html)$"

echo ""
echo "========================================"
echo "  Starting HTTP Server on http://localhost:8080"
echo "========================================"

# Start Python HTTP server
cd "$WEB_RUN_DIR"
python3 -m http.server 8080
