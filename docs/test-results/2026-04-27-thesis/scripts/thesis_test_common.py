from __future__ import annotations

import json
import math
import os
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
RESULTS_DIR = SCRIPT_DIR.parent
REPO_ROOT = RESULTS_DIR.parents[2]
RAW_DIR = RESULTS_DIR / "raw"
TABLES_DIR = RESULTS_DIR / "tables"
CHARTS_DIR = RESULTS_DIR / "charts"
JMETER_DIR = RESULTS_DIR / "jmeter"

GATEWAY_BASE = "http://localhost:8080"
RAG_BASE = "http://localhost:9701"
LOGIN_URL = f"{GATEWAY_BASE}/v1/admin/login"

USERNAME = "18877506110"
PASSWORD = "myy030202"

JMETER_BIN = Path(r"E:\apache-jmeter-5.6.3\bin\jmeter.bat")
JAVA_BIN = Path(r"C:\Program Files\Java\jdk-22\bin\java.exe")
WORKSPACE_SUBST = Path("W:/")
RAG_CLASSPATH_FILE = Path("W:/wsb-modules/wsb-rag/target/rag-runtime-classpath-full.txt")
RAG_MAIN_CLASS = "com.wsb.rag.WsbRagApplication"

ALIYUN_ISBN_BASE_URL = "https://jmisbn.market.alicloudapi.com"
GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1"


@dataclass
class HttpResult:
    ok: bool
    status_code: int
    duration_ms: float
    data: Any
    raw_text: str
    error: str | None = None


def ensure_dir(path: Path) -> Path:
    path.mkdir(parents=True, exist_ok=True)
    return path


def ensure_standard_dirs() -> None:
    for path in (
        RAW_DIR / "functional",
        RAW_DIR / "performance",
        RAW_DIR / "rag",
        TABLES_DIR,
        CHARTS_DIR,
        JMETER_DIR,
    ):
        ensure_dir(path)


def write_json(path: Path, payload: Any) -> None:
    ensure_dir(path.parent)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    ensure_dir(path.parent)
    path.write_text(content, encoding="utf-8")


def markdown_table(headers: list[str], rows: Iterable[Iterable[Any]]) -> str:
    string_rows = [[str(cell) for cell in row] for row in rows]
    widths = [len(header) for header in headers]
    for row in string_rows:
        for index, cell in enumerate(row):
            widths[index] = max(widths[index], len(cell))

    def render(row: list[str]) -> str:
        return "| " + " | ".join(cell.ljust(widths[index]) for index, cell in enumerate(row)) + " |"

    separator = "| " + " | ".join("-" * width for width in widths) + " |"
    lines = [render(headers), separator]
    lines.extend(render(row) for row in string_rows)
    return "\n".join(lines)


def http_json(
    method: str,
    url: str,
    *,
    headers: dict[str, str] | None = None,
    body: Any | None = None,
    timeout: int = 30,
) -> HttpResult:
    request_headers = {"Accept": "application/json"}
    if headers:
        request_headers.update(headers)

    data_bytes: bytes | None = None
    if body is not None:
        data_bytes = json.dumps(body, ensure_ascii=False).encode("utf-8")
        request_headers.setdefault("Content-Type", "application/json")

    request = urllib.request.Request(url=url, data=data_bytes, headers=request_headers, method=method.upper())
    started_at = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8", errors="replace")
            elapsed_ms = (time.perf_counter() - started_at) * 1000
            return HttpResult(
                ok=200 <= response.status < 300,
                status_code=response.status,
                duration_ms=elapsed_ms,
                data=parse_json(raw),
                raw_text=raw,
            )
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        elapsed_ms = (time.perf_counter() - started_at) * 1000
        return HttpResult(
            ok=False,
            status_code=exc.code,
            duration_ms=elapsed_ms,
            data=parse_json(raw),
            raw_text=raw,
            error=f"HTTP {exc.code}",
        )
    except Exception as exc:  # pragma: no cover - diagnostic path
        elapsed_ms = (time.perf_counter() - started_at) * 1000
        return HttpResult(
            ok=False,
            status_code=0,
            duration_ms=elapsed_ms,
            data=None,
            raw_text="",
            error=str(exc),
        )


def parse_json(raw_text: str) -> Any:
    try:
        return json.loads(raw_text)
    except json.JSONDecodeError:
        return None


def login() -> tuple[str, HttpResult]:
    result = http_json("POST", LOGIN_URL, body={"username": USERNAME, "password": PASSWORD})
    if not result.ok:
        raise RuntimeError(f"login failed: {result.status_code} {result.error or result.raw_text}")

    payload = result.data or {}
    token = ((payload.get("data") or {}).get("tokenValue")) if isinstance(payload, dict) else None
    if not token:
        raise RuntimeError(f"missing token in login response: {payload}")
    return token, result


def auth_headers(token: str) -> dict[str, str]:
    return {
        "satoken": token,
        "Authorization": f"Bearer {token}",
    }


def run_command(
    args: list[str],
    *,
    cwd: Path | None = None,
    check: bool = True,
    timeout: int | None = None,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        cwd=str(cwd or REPO_ROOT),
        check=check,
        text=True,
        capture_output=True,
        timeout=timeout,
        env=env,
        encoding="utf-8",
        errors="replace",
    )


def percentile(values: list[float], percent: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    if len(ordered) == 1:
        return float(ordered[0])
    rank = (len(ordered) - 1) * percent
    lower = math.floor(rank)
    upper = math.ceil(rank)
    if lower == upper:
        return float(ordered[int(rank)])
    lower_value = ordered[lower]
    upper_value = ordered[upper]
    return float(lower_value + (upper_value - lower_value) * (rank - lower))


def powershell(command: str) -> subprocess.CompletedProcess[str]:
    return run_command(
        [
            "powershell",
            "-NoProfile",
            "-Command",
            "$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new(); " + command,
        ],
        cwd=REPO_ROOT,
    )


def ensure_subst_drive() -> None:
    if WORKSPACE_SUBST.exists():
        return
    try:
        run_command(["cmd", "/c", "subst", "W:", str(REPO_ROOT)], cwd=REPO_ROOT)
    except Exception:
        # Fall back to direct workspace paths when the sandbox blocks creating a subst drive.
        return


def read_rag_runtime_classpath() -> str:
    ensure_subst_drive()
    classpath_file = RAG_CLASSPATH_FILE if RAG_CLASSPATH_FILE.exists() else REPO_ROOT / "wsb-modules" / "wsb-rag" / "target" / "rag-runtime-classpath-full.txt"
    if not classpath_file.exists():
        raise FileNotFoundError(f"missing runtime classpath file: {classpath_file}")

    workspace_root = WORKSPACE_SUBST if WORKSPACE_SUBST.exists() else REPO_ROOT
    extra_classpath = classpath_file.read_text(encoding="utf-8").strip()
    source_entries = [
        str(workspace_root / "wsb-modules" / "wsb-rag" / "target" / "classes"),
        str(workspace_root / "wsb-api" / "wsb-api-book" / "target" / "classes"),
        str(workspace_root / "wsb-common" / "wsb-common-core" / "target" / "classes"),
        str(workspace_root / "wsb-common" / "wsb-common-mybatis" / "target" / "classes"),
        str(workspace_root / "wsb-common" / "wsb-common-auth" / "target" / "classes"),
        str(workspace_root / "wsb-common" / "wsb-common-log" / "target" / "classes"),
        str(workspace_root / "wsb-common" / "wsb-common-doc" / "target" / "classes"),
    ]
    return ";".join(source_entries + [extra_classpath])


def current_date_string() -> str:
    return time.strftime("%Y-%m-%d")
