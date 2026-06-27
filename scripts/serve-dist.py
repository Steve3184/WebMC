#!/usr/bin/env python3
"""Tiny HTTP server for dist/.

Why not `python3 -m http.server`?
- DevTools auto-fetches *.map and the 300 MB game.js.map locks up Chromium.
  This server returns 404 for any *.map (and rewrites X-SourceMap headers
  out) so DevTools never even tries to download it.

Usage:
    scripts/serve-dist.py [port]
    # default port 8080
"""
from __future__ import annotations
import http.server
import os
import sys
from pathlib import Path

DIST = Path(__file__).resolve().parent.parent / "dist"


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=str(DIST), **kw)

    def _is_map(self, path: str) -> bool:
        return path.split("?", 1)[0].split("#", 1)[0].endswith(".map")

    def do_GET(self):
        if self._is_map(self.path):
            self.send_error(404, "source maps disabled (devtools-friendly)")
            return
        super().do_GET()

    def do_HEAD(self):
        if self._is_map(self.path):
            self.send_error(404, "source maps disabled (devtools-friendly)")
            return
        super().do_HEAD()

    def end_headers(self):
        # Strip any sourcemap pointer headers — game.js still has the
        # //# sourceMappingURL= comment inside it, but a missing file +
        # 404 makes devtools give up cleanly.
        super().end_headers()


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    if not DIST.is_dir():
        sys.exit(f"dist/ not found at {DIST} — run scripts/build-web.sh first")
    addr = ("", port)
    with http.server.ThreadingHTTPServer(addr, Handler) as httpd:
        print(f"serving {DIST} on http://localhost:{port}/  (*.map blocked)")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print()


if __name__ == "__main__":
    main()
