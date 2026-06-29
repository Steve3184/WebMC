const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', 'work', 'build', 'web-run');
const port = Number(process.argv[2] || process.env.PORT || 8080);

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.bin': 'application/octet-stream',
  '.vfs': 'application/octet-stream',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png'
};

function safeJoin(reqUrl) {
  const parsed = new URL(reqUrl || '/', 'http://localhost');
  let pathname = decodeURIComponent(parsed.pathname || '/');
  if (pathname === '/' || pathname === '') pathname = '/index.html';
  const filePath = path.normalize(path.join(root, pathname));
  return filePath.startsWith(root) ? filePath : null;
}

if (!fs.existsSync(path.join(root, 'index.html'))) {
  console.error(`Missing ${path.join(root, 'index.html')}. Run npm run phase197:build first.`);
  process.exit(1);
}

const server = http.createServer((req, res) => {
  const parsed = new URL(req.url || '/', 'http://localhost');
  if (parsed.pathname === '/__webmc_state') {
    res.writeHead(204);
    res.end();
    return;
  }

  const filePath = safeJoin(req.url);
  if (!filePath || filePath.endsWith('.map')) {
    res.writeHead(404);
    res.end('Not Found');
    return;
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not Found');
      return;
    }
    res.writeHead(200, {
      'Content-Type': mime[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
      'Cache-Control': 'no-cache'
    });
    res.end(data);
  });
});

server.listen(port, '127.0.0.1', () => {
  console.log(`serving ${root}`);
  console.log(`main menu http://127.0.0.1:${port}/?boot=mcMain`);
  console.log(`enter world http://127.0.0.1:${port}/?boot=mcMain&autostart=1&world=Web%20World`);
});
