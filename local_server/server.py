import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

from storage import load_checklists


class ChecklistRequestHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send_json(self, status_code: int, payload: object) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/health":
            self._send_json(200, {"status": "ok"})
            return

        if parsed.path == "/api/checklists":
            data_file: Path = self.server.data_file
            try:
                checklists = load_checklists(data_file)
            except Exception as error:
                self._send_json(500, {"error": f"Cannot read data file: {error}"})
                return
            self._send_json(200, checklists)
            return

        self._send_json(404, {"error": "Not found"})

    def log_message(self, format: str, *args: object) -> None:
        return


class ChecklistServer(ThreadingHTTPServer):
    def __init__(self, server_address: tuple[str, int], data_file: Path):
        super().__init__(server_address, ChecklistRequestHandler)
        self.data_file = data_file


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Local checklist server")
    parser.add_argument("--host", default="0.0.0.0", help="Host to bind")
    parser.add_argument("--port", default=8080, type=int, help="Port to bind")
    parser.add_argument(
        "--data-file",
        default=str(Path(__file__).with_name("data") / "checklists.json"),
        help="Path to checklists JSON file",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    data_file = Path(args.data_file).resolve()
    server = ChecklistServer((args.host, args.port), data_file)
    print("Local checklist server started.")
    print(f"Listening on: http://{args.host}:{args.port}")
    print(f"Data file: {data_file}")
    print("Endpoints:")
    print("  GET /health")
    print("  GET /api/checklists")
    print("Press Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        print("Server stopped.")


if __name__ == "__main__":
    main()
