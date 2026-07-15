#!/usr/bin/env node
/*
 * Tiny static server used only for browser validation of the built bundle.
 * Serves files under the directory passed as the first argument (defaults to
 * `dist/FE/browser`) on the port passed as the second argument (defaults to 4500).
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve(process.argv[2] || 'dist/FE/browser');
const port = Number.parseInt(process.argv[3] || '4500', 10);

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8'
};

function send404(response) {
  response.statusCode = 404;
  response.setHeader('Content-Type', 'text/plain; charset=utf-8');
  response.end('Not found');
}

const server = http.createServer((request, response) => {
  const urlPath = decodeURIComponent((request.url || '/').split('?')[0]);
  let filePath = path.join(root, urlPath);

  if (filePath.endsWith(path.sep) || !path.extname(filePath)) {
    filePath = path.join(filePath, 'index.html');
  }

  if (!filePath.startsWith(root)) {
    send404(response);
    return;
  }

  fs.stat(filePath, (statError, stats) => {
    if (statError || !stats.isFile()) {
      send404(response);
      return;
    }

    const mime = mimeTypes[path.extname(filePath).toLowerCase()] || 'application/octet-stream';
    response.statusCode = 200;
    response.setHeader('Content-Type', mime);
    response.setHeader('Cache-Control', 'no-store');
    fs.createReadStream(filePath).pipe(response);
  });
});

server.listen(port, '127.0.0.1', () => {
  process.stdout.write(`static-server: serving ${root} on http://127.0.0.1:${port}\n`);
});
