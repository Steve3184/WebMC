#!/usr/bin/env python3
# 简单的 Python HTTP 服务器用于测试 WebMC

import http.server
import socketserver
from pathlib import Path

PORT = 8080
DIRECTORY = Path(__file__).parent / "addons" / "web"

class WebMCHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(DIRECTORY), **kwargs)

    def log_message(self, format, *args):
        # 简化日志
        pass

if __name__ == "__main__":
    print("WebMC Server starting at http://localhost:{}".format(PORT))
    print("Directory: {}".format(DIRECTORY))
    print("Press Ctrl+C to stop")

    with socketserver.TCPServer(("", PORT), WebMCHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped")
