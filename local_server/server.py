import argparse
import json
import os
import secrets
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

from storage import load_checklists, now_ms, save_checklists


def normalize_checklists_payload(payload: object) -> list[dict[str, object]]:
    if not isinstance(payload, list):
        raise ValueError("Payload must be a JSON array.")

    normalized: list[dict[str, object]] = []
    seen_ids: set[str] = set()

    for index, raw_checklist in enumerate(payload):
        if not isinstance(raw_checklist, dict):
            raise ValueError(f"Checklist at index {index} must be an object.")

        checklist_id = str(raw_checklist.get("id", "")).strip()
        title = str(raw_checklist.get("title", "")).strip()
        raw_items = raw_checklist.get("items", [])
        raw_updated_at = raw_checklist.get("updatedAt")

        if not checklist_id:
            raise ValueError(f"Checklist at index {index} must contain a non-empty id.")
        if checklist_id in seen_ids:
            raise ValueError(f"Checklist id '{checklist_id}' is duplicated in payload.")
        if not title:
            raise ValueError(f"Checklist at index {index} must contain a non-empty title.")
        if not isinstance(raw_items, list):
            raise ValueError(f"Checklist '{title}' must contain an items array.")

        try:
            updated_at = int(raw_updated_at) if raw_updated_at is not None else now_ms()
        except (TypeError, ValueError) as error:
            raise ValueError(f"Checklist '{title}' has an invalid updatedAt value.") from error

        items: list[dict[str, str]] = []
        for item_index, raw_item in enumerate(raw_items):
            if not isinstance(raw_item, dict):
                raise ValueError(
                    f"Item {item_index} in checklist '{title}' must be an object.",
                )
            name = str(raw_item.get("name", "")).strip()
            if name:
                items.append({"name": name})

        normalized.append(
            {
                "id": checklist_id,
                "title": title,
                "updatedAt": updated_at,
                "items": items,
            },
        )
        seen_ids.add(checklist_id)

    return normalized


class ChecklistRequestHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send_json(self, status_code: int, payload: object) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, PUT, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.end_headers()
        self.wfile.write(body)

    def _read_json_body(self) -> object:
        content_length = int(self.headers.get("Content-Length") or "0")
        raw_body = self.rfile.read(content_length) if content_length > 0 else b""
        if not raw_body:
            raise ValueError("Request body is empty.")

        try:
            return json.loads(raw_body.decode("utf-8"))
        except json.JSONDecodeError as error:
            raise ValueError(f"Invalid JSON: {error.msg}") from error

    def _send_unauthorized(self) -> None:
        body = json.dumps({"error": "Unauthorized"}, ensure_ascii=False).encode("utf-8")
        self.send_response(401)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, PUT, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.send_header("WWW-Authenticate", 'Bearer realm="store-checklist"')
        self.end_headers()
        self.wfile.write(body)

    def _extract_bearer_token(self) -> str:
        header = self.headers.get("Authorization", "")
        scheme, _, token = header.partition(" ")
        if scheme.lower() != "bearer":
            return ""
        return token.strip()

    def _is_authorized(self, required_token: str) -> bool:
        if not required_token:
            return True
        provided_token = self._extract_bearer_token()
        if not provided_token:
            return False
        return secrets.compare_digest(provided_token, required_token)

    def do_OPTIONS(self) -> None:
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, PUT, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/health":
            self._send_json(200, {"status": "ok"})
            return

        if parsed.path == "/api/checklists":
            if not self._is_authorized(self.server.read_token):
                self._send_unauthorized()
                return

            data_file: Path = self.server.data_file
            try:
                checklists = load_checklists(data_file)
            except Exception as error:
                self._send_json(500, {"error": f"Cannot read data file: {error}"})
                return
            self._send_json(200, checklists)
            return

        self._send_json(404, {"error": "Not found"})

    def do_PUT(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path != "/api/checklists":
            self._send_json(404, {"error": "Not found"})
            return

        if not self._is_authorized(self.server.write_token):
            self._send_unauthorized()
            return

        data_file: Path = self.server.data_file
        try:
            payload = self._read_json_body()
            normalized_checklists = normalize_checklists_payload(payload)
            save_checklists(data_file, normalized_checklists)
        except ValueError as error:
            self._send_json(400, {"error": str(error)})
            return
        except Exception as error:
            self._send_json(500, {"error": f"Cannot save data file: {error}"})
            return

        self._send_json(200, normalized_checklists)

    def log_message(self, format: str, *args: object) -> None:
        return


class ChecklistServer(ThreadingHTTPServer):
    def __init__(
        self,
        server_address: tuple[str, int],
        data_file: Path,
        read_token: str,
        write_token: str,
    ):
        super().__init__(server_address, ChecklistRequestHandler)
        self.data_file = data_file
        self.read_token = read_token.strip()
        self.write_token = write_token.strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Checklist server")
    parser.add_argument(
        "--host",
        default=os.getenv("CHECKLIST_SERVER_HOST", "0.0.0.0"),
        help="Host to bind",
    )
    parser.add_argument(
        "--port",
        default=int(os.getenv("CHECKLIST_SERVER_PORT", os.getenv("PORT", "8080"))),
        type=int,
        help="Port to bind",
    )
    parser.add_argument(
        "--data-file",
        default=os.getenv(
            "CHECKLIST_SERVER_DATA_FILE",
            str(Path(__file__).with_name("data") / "checklists.json"),
        ),
        help="Path to checklists JSON file",
    )
    parser.add_argument(
        "--read-token",
        default=os.getenv(
            "CHECKLIST_SERVER_READ_TOKEN",
            os.getenv("CHECKLIST_SERVER_AUTH_TOKEN", ""),
        ),
        help="Bearer token required for GET /api/checklists",
    )
    parser.add_argument(
        "--write-token",
        default=os.getenv(
            "CHECKLIST_SERVER_WRITE_TOKEN",
            os.getenv(
                "CHECKLIST_SERVER_AUTH_TOKEN",
                os.getenv("CHECKLIST_SERVER_READ_TOKEN", ""),
            ),
        ),
        help="Bearer token required for PUT /api/checklists",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    data_file = Path(args.data_file).resolve()
    server = ChecklistServer(
        (args.host, args.port),
        data_file,
        read_token=args.read_token,
        write_token=args.write_token,
    )
    print("Checklist server started.")
    print(f"Listening on: http://{args.host}:{args.port}")
    print(f"Data file: {data_file}")
    print(f"Read token: {'enabled' if server.read_token else 'disabled'}")
    print(f"Write token: {'enabled' if server.write_token else 'disabled'}")
    print("Endpoints:")
    print("  GET /health")
    print("  GET /api/checklists")
    print("  PUT /api/checklists")
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
