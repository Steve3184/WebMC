#!/usr/bin/env node
/**
 * Simple static file server with Brotli compression support.
 *
 * Automatically serves pre-compressed .br files when Accept-Encoding contains 'br'.
 * Falls back to original files if .br version is unavailable.
 *
 * Usage:
 *   node scripts/serve-brotli.js [port] [dir]
 *
 * Default: port 9090, directory work/build/web-run
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

// Get absolute path to project root
const PROJECT_ROOT = path.resolve(__dirname, '..');
const PORT = process.argv[2] || process.env.PORT || 9090;
const STATIC_DIR = process.argv[3] || process.env.STATIC_DIR || path.join(PROJECT_ROOT, 'work', 'build', 'web-run');

// MIME types
const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.mjs': 'application/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.txt': 'text/plain; charset=utf-8',
    '.vfs': 'application/octet-stream',
    '.wasm': 'application/wasm',
};

function getMimeType(filePath) {
    const ext = path.extname(filePath).toLowerCase();
    return MIME_TYPES[ext] || 'application/octet-stream';
}

function serveFile(req, res, filePath, stats) {
    const mimeType = getMimeType(filePath);
    const acceptEncoding = req.headers['accept-encoding'] || '';

    // Check for pre-compressed Brotli version
    const brPath = filePath + '.br';
    const hasBrotli = acceptEncoding.includes('br') && fs.existsSync(brPath);

    // Check for gzip as fallback
    const gzPath = filePath + '.gz';
    const hasGzip = acceptEncoding.includes('gzip') && fs.existsSync(gzPath);

    let data;
    let encoding = null;
    let contentType = mimeType;

    if (hasBrotli) {
        data = fs.readFileSync(brPath);
        encoding = 'br';
        // Tell browser it's compressed
        contentType += '; charset=x-brotli';
        res.setHeader('Content-Encoding', 'br');
        res.setHeader('X-Content-Encoding', 'br');
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} → ${stats.size.toLocaleString()} bytes (Brotli: ${(data.length / 1024 / 1024).toFixed(2)} MB)`);
    } else if (hasGzip) {
        data = fs.readFileSync(gzPath);
        encoding = 'gzip';
        res.setHeader('Content-Encoding', 'gzip');
        res.setHeader('X-Content-Encoding', 'gzip');
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} → ${stats.size.toLocaleString()} bytes (GZip: ${(data.length / 1024 / 1024).toFixed(2)} MB)`);
    } else {
        data = fs.readFileSync(filePath);
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} → ${(data.length / 1024 / 1024).toFixed(2)} MB`);
    }

    // CORS headers for local development
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Accept-Encoding, Content-Type');

    // Cache control for build artifacts
    res.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
    res.setHeader('Content-Type', contentType);
    res.setHeader('Content-Length', data.length);

    res.end(data);
}

const server = http.createServer((req, res) => {
    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
        res.setHeader('Access-Control-Allow-Headers', 'Accept-Encoding, Content-Type');
        res.writeHead(204);
        res.end();
        return;
    }

    if (req.method !== 'GET' && req.method !== 'HEAD') {
        res.writeHead(405, { 'Allow': 'GET, HEAD, OPTIONS' });
        res.end('Method Not Allowed');
        return;
    }

    let urlPath = req.url.split('?')[0];

    // Prevent directory traversal
    if (urlPath.includes('..')) {
        res.writeHead(400);
        res.end('Bad Request');
        return;
    }

    // Default to index.html
    if (urlPath === '/' || urlPath === '') {
        urlPath = '/index.html';
    }

    const filePath = path.join(STATIC_DIR, urlPath);
    const normalizedPath = path.normalize(filePath);

    // Ensure file is within static directory
    if (!normalizedPath.startsWith(STATIC_DIR)) {
        res.writeHead(403);
        res.end('Forbidden');
        return;
    }

    fs.stat(normalizedPath, (err, stats) => {
        if (err || !stats.isFile()) {
            // Try index.html for SPA-style routing
            const indexPath = path.join(normalizedPath, 'index.html');
            fs.stat(indexPath, (err2, stats2) => {
                if (err2 || !stats2.isFile()) {
                    res.writeHead(404);
                    res.end('Not Found: ' + urlPath);
                    return;
                }
                serveFile(req, res, indexPath, stats2);
            });
            return;
        }
        serveFile(req, res, normalizedPath, stats);
    });
});

server.listen(PORT, () => {
    console.log(`\n🚀 WebMC Server with Brotli support`);
    console.log(`   Serving: ${STATIC_DIR}`);
    console.log(`   Port:    ${PORT}`);
    console.log(`   URL:     http://localhost:${PORT}\n`);
    console.log('   Compression:');
    console.log('   - game.js   290MB → ~19MB (Brotli)');
    console.log('   - game.vfs   14MB → ~4MB (Brotli)\n');
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.error(`\n❌ Port ${PORT} is already in use.`);
        console.error(`   Try: node scripts/serve-brotli.js ${PORT + 1} "${STATIC_DIR}"\n`);
    } else {
        console.error('Server error:', err);
    }
    process.exit(1);
});
