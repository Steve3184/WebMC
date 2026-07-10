// compressed-server.js - Simple static file server with compression
// Usage: node compressed-server.js [port]

const http = require('http');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const PORT = process.argv[2] || 8080;
const SERVE_DIR = __dirname;

const MIME_TYPES = {
    '.html': 'text/html',
    '.js': 'application/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.txt': 'text/plain',
    '.vfs': 'application/octet-stream',
};

const server = http.createServer((req, res) => {
    // Parse URL and remove query string
    let urlPath = req.url.split('?')[0];
    if (urlPath === '/') urlPath = '/index.html';

    const filePath = path.join(SERVE_DIR, urlPath);
    const ext = path.extname(filePath).toLowerCase();
    const mimeType = MIME_TYPES[ext] || 'application/octet-stream';

    // Security: prevent directory traversal
    if (!filePath.startsWith(SERVE_DIR)) {
        res.writeHead(403);
        res.end('Forbidden');
        return;
    }

    // Check for pre-compressed versions
    const acceptEncoding = req.headers['accept-encoding'] || '';
    let compressed = false;
    let actualFilePath = filePath;

    if (acceptEncoding.includes('br') && fs.existsSync(filePath + '.br')) {
        actualFilePath = filePath + '.br';
        res.setHeader('Content-Encoding', 'br');
        compressed = true;
        console.log(`[200] ${urlPath} (Brotli)`);
    } else if (acceptEncoding.includes('gzip') && fs.existsSync(filePath + '.gz')) {
        actualFilePath = filePath + '.gz';
        res.setHeader('Content-Encoding', 'gzip');
        compressed = true;
        console.log(`[200] ${urlPath} (Gzip)`);
    } else {
        console.log(`[200] ${urlPath}`);
    }

    fs.readFile(actualFilePath, (err, data) => {
        if (err) {
            if (err.code === 'ENOENT') {
                res.writeHead(404);
                res.end('Not Found');
            } else {
                res.writeHead(500);
                res.end('Server Error');
            }
            return;
        }
        res.writeHead(200, {
            'Content-Type': mimeType,
            'Cache-Control': 'public, max-age=31536000',
        });
        res.end(data);
    });
});

server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
    console.log('Serving files from:', SERVE_DIR);
    console.log('Supports automatic Brotli (.br) and Gzip (.gz) compression');
});
