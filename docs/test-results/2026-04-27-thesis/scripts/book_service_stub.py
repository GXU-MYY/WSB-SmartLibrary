from __future__ import annotations

import argparse
import base64
import json
import logging
import os
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import parse_qs, urlparse


logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
LOGGER = logging.getLogger("book-service-stub")
MYSQL_BIN = os.environ.get("WSB_MYSQL_BIN", r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe")


def mysql_json_rows(query: str) -> list[dict[str, Any]]:
    command = [
        MYSQL_BIN,
        "-h",
        "127.0.0.1",
        "-P",
        "3306",
        "--default-character-set=utf8mb4",
        "-uroot",
        "-p123456",
        "-D",
        "WSB",
        "-N",
        "-B",
        "-e",
        query,
    ]
    result = subprocess.run(
        command,
        check=True,
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )
    rows: list[dict[str, Any]] = []
    for line in result.stdout.splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(base64.b64decode(line).decode("utf-8")))
    return rows


def load_books() -> dict[int, dict[str, Any]]:
    query = """
    SELECT REPLACE(TO_BASE64(JSON_OBJECT(
        'id', id,
        'title', title,
        'subtitle', subtitle,
        'author', author,
        'summary', summary,
        'publisher', publisher,
        'isbn', isbn,
        'isbn10', isbn10,
        'keyword', keyword,
        'label', label,
        'clc', clc,
        'coverUrl', cover_url,
        'embeddingStatus', embedding_status,
        'userId', user_id,
        'isBorrowed', is_borrowed,
        'isLentOut', is_lent_out
    )), '\n', '')
    FROM t_book
    WHERE is_deleted = 0;
    """
    books: dict[int, dict[str, Any]] = {}
    for row in mysql_json_rows(query):
        book_id = row.get("id")
        if book_id is None:
            continue
        row["isBorrowed"] = bool(row.get("isBorrowed"))
        row["isLentOut"] = bool(row.get("isLentOut"))
        books[int(book_id)] = row
    LOGGER.info("loaded %s books into stub cache", len(books))
    return books


BOOKS_BY_ID = load_books()


def parse_ids(query_params: dict[str, list[str]]) -> list[int]:
    raw_values = query_params.get("ids", [])
    resolved: list[int] = []
    for raw in raw_values:
        for part in raw.split(","):
            text = part.strip()
            if not text:
                continue
            try:
                resolved.append(int(text))
            except ValueError:
                continue
    return resolved


def write_json(handler: BaseHTTPRequestHandler, status_code: int, payload: dict[str, Any]) -> None:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status_code)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(data)))
    handler.end_headers()
    handler.wfile.write(data)


class BookStubHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        if parsed.path == "/health":
            write_json(self, 200, {"status": "ok", "books": len(BOOKS_BY_ID)})
            return

        if parsed.path == "/v1/inner/book/batch":
            ids = parse_ids(parse_qs(parsed.query, keep_blank_values=True))
            books = [BOOKS_BY_ID[book_id] for book_id in ids if book_id in BOOKS_BY_ID]
            write_json(self, 200, {"code": 200, "msg": "操作成功", "data": books})
            return

        if parsed.path.startswith("/v1/inner/book/"):
            try:
                book_id = int(parsed.path.rsplit("/", 1)[-1])
            except ValueError:
                write_json(self, 404, {"code": 404, "msg": "图书不存在", "data": None})
                return
            write_json(self, 200, {"code": 200, "msg": "操作成功", "data": BOOKS_BY_ID.get(book_id)})
            return

        write_json(self, 404, {"code": 404, "msg": "not found", "data": None})

    def log_message(self, format: str, *args: Any) -> None:
        LOGGER.info("%s - %s", self.address_string(), format % args)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=19001)
    args = parser.parse_args()

    server = ThreadingHTTPServer(("127.0.0.1", args.port), BookStubHandler)
    LOGGER.info("book stub listening on http://127.0.0.1:%s", args.port)
    server.serve_forever()


if __name__ == "__main__":
    main()
