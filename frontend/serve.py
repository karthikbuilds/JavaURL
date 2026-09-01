#!/usr/bin/env python3
"""
Zero-build dev server for the JavaURL frontend.

Adds no-cache headers so the browser never serves a stale copy of config.js /
index.html after you change the API port. Usage:

    python3 serve.py [port]
"""
import functools
import http.server
import socketserver
import sys

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 3001


class NoCacheHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()


socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(('', PORT), functools.partial(NoCacheHandler, directory='.')) as httpd:
    print(f'JavaURL frontend serving on http://localhost:{PORT} (no-cache)', flush=True)
    httpd.serve_forever()
