#!/usr/bin/env python3
"""GC Coffee 浏览器预览静态服务器（带 no-cache 头，改动即时生效）"""
import http.server
import os
import socketserver

PORT = 8090
ROOT = os.path.dirname(os.path.abspath(__file__))
os.chdir(ROOT)


class Handler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Pragma', 'no-cache')
        super().end_headers()


socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(('127.0.0.1', PORT), Handler) as httpd:
    print(f'GC Coffee preview: http://127.0.0.1:{PORT} (Ctrl+C 停止)')
    httpd.serve_forever()
