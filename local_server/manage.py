import argparse
import uuid
from pathlib import Path

from storage import load_checklists, now_ms, save_checklists


def parse_items(raw_items: list[str]) -> list[dict[str, str]]:
    items: list[dict[str, str]] = []
    for raw in raw_items:
        name = raw.strip()
        if name:
            items.append({"name": name})
    return items


def get_default_data_file() -> Path:
    return Path(__file__).with_name("data") / "checklists.json"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Manage local checklist server data")
    parser.add_argument(
        "--data-file",
        default=str(get_default_data_file()),
        help="Path to checklists JSON file",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("list", help="Print all checklists")

    add_list = subparsers.add_parser("add-list", help="Add a new checklist")
    add_list.add_argument("--title", required=True, help="Checklist title")
    add_list.add_argument(
        "--item",
        action="append",
        default=[],
        help="Item name. Repeat the flag for multiple items.",
    )

    delete_list = subparsers.add_parser("delete-list", help="Delete checklist by id")
    delete_list.add_argument("--id", required=True, help="Checklist id")

    rename_list = subparsers.add_parser("rename-list", help="Rename checklist by id")
    rename_list.add_argument("--id", required=True, help="Checklist id")
    rename_list.add_argument("--title", required=True, help="New title")

    replace_items = subparsers.add_parser(
        "replace-items",
        help="Replace checklist items by id",
    )
    replace_items.add_argument("--id", required=True, help="Checklist id")
    replace_items.add_argument(
        "--item",
        action="append",
        default=[],
        help="Item name. Repeat the flag for multiple items.",
    )

    return parser


def cmd_list(checklists: list[dict]) -> int:
    if not checklists:
        print("No checklists in data file.")
        return 0
    for checklist in checklists:
        title = checklist.get("title", "")
        checklist_id = checklist.get("id", "")
        updated_at = checklist.get("updatedAt", "")
        items = checklist.get("items", [])
        print(f"- id={checklist_id}, title={title}, items={len(items)}, updatedAt={updated_at}")
    return 0


def cmd_add_list(checklists: list[dict], title: str, items: list[str]) -> int:
    normalized_title = title.strip()
    if not normalized_title:
        print("Title cannot be empty.")
        return 1
    checklist = {
        "id": str(uuid.uuid4()),
        "title": normalized_title,
        "updatedAt": now_ms(),
        "items": parse_items(items),
    }
    checklists.append(checklist)
    print(f"Checklist created: id={checklist['id']}")
    return 0


def find_by_id(checklists: list[dict], checklist_id: str) -> dict | None:
    for checklist in checklists:
        if checklist.get("id") == checklist_id:
            return checklist
    return None


def cmd_delete_list(checklists: list[dict], checklist_id: str) -> int:
    before = len(checklists)
    checklists[:] = [item for item in checklists if item.get("id") != checklist_id]
    if len(checklists) == before:
        print("Checklist not found.")
        return 1
    print("Checklist deleted.")
    return 0


def cmd_rename_list(checklists: list[dict], checklist_id: str, title: str) -> int:
    checklist = find_by_id(checklists, checklist_id)
    if checklist is None:
        print("Checklist not found.")
        return 1
    normalized_title = title.strip()
    if not normalized_title:
        print("Title cannot be empty.")
        return 1
    checklist["title"] = normalized_title
    checklist["updatedAt"] = now_ms()
    print("Checklist renamed.")
    return 0


def cmd_replace_items(checklists: list[dict], checklist_id: str, items: list[str]) -> int:
    checklist = find_by_id(checklists, checklist_id)
    if checklist is None:
        print("Checklist not found.")
        return 1
    checklist["items"] = parse_items(items)
    checklist["updatedAt"] = now_ms()
    print(f"Checklist items replaced. New count: {len(checklist['items'])}")
    return 0


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    data_file = Path(args.data_file).resolve()
    checklists = load_checklists(data_file)

    if args.command == "list":
        return cmd_list(checklists)

    if args.command == "add-list":
        code = cmd_add_list(checklists, args.title, args.item)
    elif args.command == "delete-list":
        code = cmd_delete_list(checklists, args.id)
    elif args.command == "rename-list":
        code = cmd_rename_list(checklists, args.id, args.title)
    elif args.command == "replace-items":
        code = cmd_replace_items(checklists, args.id, args.item)
    else:
        print("Unsupported command.")
        return 1

    if code == 0:
        save_checklists(data_file, checklists)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
