import json
import time
from pathlib import Path
from typing import Any


def ensure_data_file(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_text("[]\n", encoding="utf-8")


def load_checklists(path: Path) -> list[dict[str, Any]]:
    ensure_data_file(path)
    raw = path.read_text(encoding="utf-8").strip()
    if not raw:
        return []
    data = json.loads(raw)
    if not isinstance(data, list):
        raise ValueError("Data file must contain a JSON array.")
    return data


def save_checklists(path: Path, checklists: list[dict[str, Any]]) -> None:
    ensure_data_file(path)
    path.write_text(
        json.dumps(checklists, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def now_ms() -> int:
    return int(time.time() * 1000)
