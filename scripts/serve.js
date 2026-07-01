const http = require('http');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const PORT = process.argv[2] || 8080;
const STATIC_DIR = path.join(__dirname, '..', 'build', 'web-run');

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
  '.vfs': 'application/octet-stream',
};

const server = http.createServer((req, res) => {
  // Parse URL and remove query strings
  let urlPath = req.url.split('?')[0];
  if (urlPath === '/') urlPath = '/index.html';

  const filePath = path.join(STATIC_DIR, urlPath);
  const ext = path.extname(filePath).toLowerCase();
  const mimeType = MIME_TYPES[ext] || 'application/octet-stream';

  // Check for pre-compressed Brotli version first
  const brotliFile = filePath + '.br';
  const acceptEncoding = req.headers['accept-encoding'] || '';

  // Serve brotli if available and client accepts it
  if (acceptEncoding.includes('br') && fs.existsSync(brotliFile)) {
    const stats = fs.statSync(brotliFile);
    res.writeHead(200, {
      'Content-Type': mimeType,
      'Content-Encoding': 'br',
      'Content-Length': stats.size,
      'X-Original-Size': fs.statSync(filePath).size,
      'X-Compression-Ratio': ((1 - stats.size / fs.statSync(filePath).size) * 100).toFixed(1) + '%',
    });
    fs.createReadStream(brotliFile).pipe(res);
    return;
  }

  // Serve gzip if available and client accepts it
  const gzipFile = filePath + '.gz';
  if (acceptEncoding.includes('gzip') && fs.existsSync(gzipFile)) {
    const stats = fs.statSync(gzipFile);
    res.writeHead(200, {
      'Content-Type': mimeType,
      'Content-Encoding': 'gzip',
      'Content-Length': stats.size,
    });
    fs.createReadStream(gzipFile).pipe(res);
    return;
  }

  // Serve uncompressed file
  if (fs.existsSync(filePath)) {
    const stats = fs.statSync(filePath);
    res.writeHead(200, {
      'Content-Type': mimeType,
      'Content-Length': stats.size,
    });
    fs.createReadStream(filePath).pipe(res);
    return;
  }

  // 404
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('404 Not Found: ' + urlPath);
});

server.listen(PORT, () => {
  console.log(`[serve] Server running at http://localhost:${PORT}/`);
  console.log(`[serve] Serving from: ${STATIC_DIR}`);
});
