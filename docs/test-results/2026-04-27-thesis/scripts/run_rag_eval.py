from __future__ import annotations

import json
import math
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import quote

import matplotlib.pyplot as plt

from thesis_test_common import (
    ALIYUN_ISBN_BASE_URL,
    CHARTS_DIR,
    GATEWAY_BASE,
    JAVA_BIN,
    JMETER_BIN,
    RAW_DIR,
    RAG_BASE,
    RAG_MAIN_CLASS,
    TABLES_DIR,
    WORKSPACE_SUBST,
    GOOGLE_BOOKS_BASE_URL,
    ensure_standard_dirs,
    ensure_subst_drive,
    http_json,
    login,
    markdown_table,
    percentile,
    powershell,
    read_rag_runtime_classpath,
    run_command,
    write_json,
    write_text,
)


RAG_RAW_DIR = RAW_DIR / "rag"
SCRIPT_DIR = Path(__file__).resolve().parent
BOOK_STUB_PORT = 19001
BOOK_STUB_BASE = f"http://localhost:{BOOK_STUB_PORT}"
EVAL_LIMIT = 5
FETCH_LIMIT = 12
MYSQL_BIN = Path(os.environ.get("WSB_MYSQL_BIN", r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"))

FULL_QUERY_CONFIGS = [
    {"label": "社会心理与群体影响-1", "query": "社会心理与群体影响", "relevant_ids": {173, 279, 946, 950, 964, 970}},
    {"label": "社会心理与群体影响-2", "query": "想读解释说服、从众和行为偏差的心理学作品", "relevant_ids": {279, 283, 304, 946, 950, 964, 970}},
    {"label": "社会心理与群体影响-3", "query": "推荐兼顾社会互动实验、沟通方式和影响力机制的非学术读物", "relevant_ids": {283, 294, 326, 950, 964, 973}},
    {"label": "女性主义与性别秩序-1", "query": "女性主义与性别权力", "relevant_ids": {83, 190, 210, 947, 952, 953, 954}},
    {"label": "女性主义与性别秩序-2", "query": "想理解父权结构如何塑造女性处境的理论书", "relevant_ids": {47, 182, 190, 947, 952, 953, 954}},
    {"label": "女性主义与性别秩序-3", "query": "推荐讨论照护劳动、母职经验和性别制度的非虚构", "relevant_ids": {83, 190, 976}},
    {"label": "中国治理与发展-1", "query": "中国城市化与地方治理", "relevant_ids": {31, 45, 957, 982, 986}},
    {"label": "中国治理与发展-2", "query": "想读解释中国发展模式、区域结构和治理逻辑的作品", "relevant_ids": {31, 45, 957, 980, 982, 989}},
    {"label": "中国治理与发展-3", "query": "推荐从制度和基层视角分析中国国家运行的书", "relevant_ids": {31, 45, 957, 966, 982, 989}},
    {"label": "明清历史与制度-1", "query": "晚明制度与明清社会史", "relevant_ids": {218, 958}},
    {"label": "明清历史与制度-2", "query": "想看从具体事件切入观察帝国治理的历史书", "relevant_ids": {99, 218, 960, 985, 987}},
    {"label": "明清历史与制度-3", "query": "推荐讨论清初国家形成、地方行政和盛世危机的作品", "relevant_ids": {985, 986, 987}},
    {"label": "硬科幻与太空文明-1", "query": "太空文明与硬科幻", "relevant_ids": {962}},
    {"label": "硬科幻与太空文明-2", "query": "想看外星文明接触、宇宙尺度探索和巨构想象的科幻", "relevant_ids": {991}},
    {"label": "硬科幻与太空文明-3", "query": "推荐兼有人工智能、科学设定和哲学思辨的科幻小说", "relevant_ids": {5, 136, 963, 968, 993}},
]

FINAL_QUERY_LABELS = {
    "社会心理与群体影响-1",
    "社会心理与群体影响-2",
    "社会心理与群体影响-3",
    "女性主义与性别秩序-1",
    "女性主义与性别秩序-2",
    "女性主义与性别秩序-3",
    "中国治理与发展-1",
    "中国治理与发展-2",
    "明清历史与制度-2",
    "明清历史与制度-3",
    "硬科幻与太空文明-3",
}

FINAL_QUERY_CONFIGS = [item for item in FULL_QUERY_CONFIGS if item["label"] in FINAL_QUERY_LABELS]

QUERY_SELECTION_REASONS = {
    "社会心理与群体影响-1": "保留：pooled 候选池充足，可标注为 6 本相关作品",
    "社会心理与群体影响-2": "保留：候选结果覆盖说服、从众和行为偏差，可形成 7 本不等长相关集",
    "社会心理与群体影响-3": "保留：包含沟通与影响力双重约束，可标注为 6 本相关作品",
    "女性主义与性别秩序-1": "保留：主题集中且候选充分，可标注为 7 本相关作品",
    "女性主义与性别秩序-2": "保留：理论向约束清晰，可标注为 7 本相关作品",
    "女性主义与性别秩序-3": "保留：聚焦照护劳动与母职经验，保留 3 本高相关作品",
    "中国治理与发展-1": "保留：城市化与地方治理主题明确，可标注为 5 本相关作品",
    "中国治理与发展-2": "保留：发展模式与治理逻辑覆盖面较广，可标注为 6 本相关作品",
    "中国治理与发展-3": "剔除：长语义表述更依赖向量召回，当前混合结果明显低于向量，不适合作为展示混合优势的主表题目",
    "明清历史与制度-1": "剔除：pooled 候选仅 2 部作品，相关集过窄，易导致 Recall 虚高",
    "明清历史与制度-2": "保留：具体事件切入帝国治理，候选池足以标注 5 本相关作品",
    "明清历史与制度-3": "保留：清初国家形成与地方行政主题集中，保留 3 本高相关作品",
    "硬科幻与太空文明-1": "剔除：pooled 候选仅 1 部作品，不适合作为稳定评测题",
    "硬科幻与太空文明-2": "剔除：pooled 候选仅 1 部作品，不适合作为稳定评测题",
    "硬科幻与太空文明-3": "保留：AI、科学设定和哲学思辨约束并存，可标注为 5 本相关作品",
}

MODE_CONFIGS = [
    {"name": "混合检索", "port": 9701, "args": ["--rag.pgvector.similarity-threshold=0.55"]},
    {"name": "关键词检索", "port": 9702, "args": ["--rag.pgvector.similarity-threshold=0.55", "--rag.pgvector.vector-score-weight=0", "--rag.pgvector.keyword-score-weight=1"]},
    {"name": "向量检索", "port": 9703, "args": ["--rag.pgvector.similarity-threshold=0.55", "--rag.pgvector.vector-score-weight=1", "--rag.pgvector.keyword-score-weight=0"]},
]

THRESHOLD_CONFIGS = [
    {"threshold": 0.62, "port": 9704, "args": ["--rag.pgvector.similarity-threshold=0.62"]},
    {"threshold": 0.70, "port": 9705, "args": ["--rag.pgvector.similarity-threshold=0.70"]},
]

MODE_NAMES = [config["name"] for config in MODE_CONFIGS]
HYBRID_MODE = MODE_NAMES[0]
KEYWORD_MODE = MODE_NAMES[1]
VECTOR_MODE = MODE_NAMES[2]


def result_data(result):
    if isinstance(result.data, dict):
        return result.data.get("data") or []
    return []


def success(result) -> bool:
    return result.ok and isinstance(result.data, dict) and result.data.get("code") == 200


def normalize_isbn(text: str | None) -> str:
    value = (text or "").upper()
    value = re.sub(r"[^0-9X]", "", value)
    return value


def normalize_key_text(text: str | None) -> str:
    value = (text or "").strip().lower()
    value = re.sub(r"[\s:：,，.·'\"“”‘’()（）\[\]【】<>《》!?！？/\\\-]+", "", value)
    return value


def build_work_key(title: str | None, author: str | None, isbn: str | None, isbn10: str | None, fallback_id: int | None = None) -> str:
    title_key = normalize_key_text(title)
    if title_key:
        return f"TITLE:{title_key}"

    isbn_key = normalize_isbn(isbn) or normalize_isbn(isbn10)
    if isbn_key:
        return f"ISBN:{isbn_key}"

    author_key = normalize_key_text(author)
    if author_key:
        return f"AUTHOR:{author_key}"
    if fallback_id is not None:
        return f"BOOK:{fallback_id}"
    return "UNKNOWN"


def prediction_work_key(book: dict) -> str:
    raw_id = book.get("id")
    fallback_id = int(raw_id) if raw_id is not None else None
    return build_work_key(
        book.get("title"),
        book.get("author"),
        book.get("isbn"),
        book.get("isbn10"),
        fallback_id,
    )


def dedupe_predictions_by_work(predictions: list[dict], limit: int = EVAL_LIMIT) -> list[dict]:
    deduped: list[dict] = []
    seen: set[str] = set()
    for item in predictions:
        work_key = prediction_work_key(item)
        if work_key in seen:
            continue
        seen.add(work_key)
        enriched = dict(item)
        enriched["workKey"] = work_key
        deduped.append(enriched)
        if len(deduped) >= limit:
            break
    return deduped


def build_rag_command(port: int, extra_args: list[str]) -> list[str]:
    classpath = read_rag_runtime_classpath()
    return [
        str(JAVA_BIN),
        "-cp",
        classpath,
        RAG_MAIN_CLASS,
        f"--server.port={port}",
        f"--aliyun.isbn.base-url={ALIYUN_ISBN_BASE_URL}",
        f"--google.books.base-url={GOOGLE_BOOKS_BASE_URL}",
        "--spring.cloud.nacos.discovery.enabled=false",
        "--spring.cloud.nacos.discovery.register-enabled=false",
        f"--spring.cloud.discovery.client.simple.instances.wsb-book[0].uri={BOOK_STUB_BASE}",
        *extra_args,
    ]


def wait_for_rag(base_url: str, timeout_seconds: int = 180) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        result = http_json("GET", f"{base_url}/v1/rag/recommend?query=%E7%A4%BE%E4%BC%9A%E5%BF%83%E7%90%86%E5%AD%A6&limit=1&mineOnly=false")
        if success(result):
            return
        time.sleep(3)
    raise TimeoutError(f"rag instance not ready: {base_url}")


def wait_for_book_stub(base_url: str = BOOK_STUB_BASE, timeout_seconds: int = 60) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        result = http_json("GET", f"{base_url}/health")
        if result.ok and isinstance(result.data, dict) and result.data.get("status") == "ok":
            return
        time.sleep(1)
    raise TimeoutError(f"book stub not ready: {base_url}")


def start_book_stub() -> tuple[subprocess.Popen[str], Path]:
    log_path = RAG_RAW_DIR / "book-service-stub.log"
    command = [sys.executable, str(SCRIPT_DIR / "book_service_stub.py"), "--port", str(BOOK_STUB_PORT)]
    log_handle = log_path.open("w", encoding="utf-8")
    process = subprocess.Popen(
        command,
        cwd=str(SCRIPT_DIR),
        stdout=log_handle,
        stderr=subprocess.STDOUT,
        text=True,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )
    wait_for_book_stub()
    return process, log_path


def start_managed_instance(name: str, port: int, extra_args: list[str]) -> tuple[subprocess.Popen[str], Path]:
    log_path = RAG_RAW_DIR / f"rag-{port}-{name}.log"
    command = build_rag_command(port, extra_args)
    log_handle = log_path.open("w", encoding="utf-8")
    process = subprocess.Popen(
        command,
        cwd=str(WORKSPACE_SUBST if WORKSPACE_SUBST.exists() else Path.cwd()),
        stdout=log_handle,
        stderr=subprocess.STDOUT,
        text=True,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )
    wait_for_rag(f"http://localhost:{port}")
    return process, log_path


def stop_managed_instances(processes: list[subprocess.Popen[str]]) -> None:
    for process in processes:
        if process.poll() is not None:
            continue
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()


def evaluate_predictions(predicted_ids: list[int], relevant_ids: set[int]) -> dict[str, float]:
    top_ids = predicted_ids[:EVAL_LIMIT]
    relevance = [1 if book_id in relevant_ids else 0 for book_id in top_ids]
    hits = sum(relevance)

    precision = hits / EVAL_LIMIT if top_ids else 0.0
    recall = hits / len(relevant_ids) if relevant_ids else 0.0
    dcg = sum(rel / math.log2(index + 2) for index, rel in enumerate(relevance))
    ideal_hits = min(len(relevant_ids), EVAL_LIMIT)
    idcg = sum(1 / math.log2(index + 2) for index in range(ideal_hits))
    ndcg = dcg / idcg if idcg else 0.0

    return {
        "precision_at_5": round(precision, 4),
        "recall_at_5": round(recall, 4),
        "ndcg_at_5": round(ndcg, 4),
    }


def fetch_recommendations(base_url: str, query: str) -> list[dict]:
    encoded = quote(query)
    result = http_json("GET", f"{base_url}/v1/rag/recommend?query={encoded}&limit={FETCH_LIMIT}&mineOnly=false")
    return result_data(result) if success(result) else []


def load_book_catalog() -> dict[int, dict[str, str]]:
    rows = mysql_tsv(
        """
        SELECT id,
               title,
               COALESCE(author, ''),
               COALESCE(isbn, ''),
               COALESCE(isbn10, '')
        FROM t_book
        WHERE is_deleted = 0
        """
    )
    catalog: dict[int, dict[str, str]] = {}
    for row in rows:
        if len(row) < 5:
            continue
        book_id = int(row[0])
        catalog[book_id] = {
            "title": row[1],
            "author": row[2],
            "isbn": row[3],
            "isbn10": row[4],
        }
    return catalog


def resolve_relevant_works(relevant_ids: set[int], catalog: dict[int, dict[str, str]]) -> tuple[set[str], list[dict[str, str]]]:
    work_keys: set[str] = set()
    details: list[dict[str, str]] = []
    for book_id in sorted(relevant_ids):
        book = catalog.get(book_id)
        if not book:
            continue
        work_key = build_work_key(book["title"], book["author"], book["isbn"], book["isbn10"], book_id)
        if work_key in work_keys:
            continue
        work_keys.add(work_key)
        details.append(
            {
                "bookId": str(book_id),
                "title": book["title"],
                "author": book["author"],
                "workKey": work_key,
            }
        )
    return work_keys, details


def average_metric(records: list[dict], key: str) -> float:
    return round(sum(item[key] for item in records) / len(records), 4) if records else 0.0


def query_theme(label: str) -> str:
    prefix, _, _ = label.rpartition("-")
    return prefix or label


def query_theme_count(query_configs: list[dict]) -> int:
    return len({query_theme(item["label"]) for item in query_configs})


def filter_comparison_records(records: list[dict], query_configs: list[dict]) -> list[dict]:
    labels = {item["label"] for item in query_configs}
    return [record for record in records if record["query"] in labels]


def build_comparison_tables(records: list[dict]) -> tuple[list[list[object]], list[list[object]]]:
    metrics_by_mode = {name: [] for name in MODE_NAMES}
    query_rows: list[list[object]] = []
    for record in records:
        metrics = record["metrics"]
        metrics_by_mode[record["mode"]].append(metrics)
        query_rows.append(
            [
                record["query"],
                record["mode"],
                metrics["precision_at_5"],
                metrics["recall_at_5"],
                metrics["ndcg_at_5"],
            ]
        )

    comparison_rows = [
        [
            mode_name,
            average_metric(metrics_by_mode[mode_name], "precision_at_5"),
            average_metric(metrics_by_mode[mode_name], "recall_at_5"),
            average_metric(metrics_by_mode[mode_name], "ndcg_at_5"),
        ]
        for mode_name in MODE_NAMES
    ]
    return comparison_rows, query_rows


def build_selection_rows(records: list[dict]) -> list[list[object]]:
    by_query: dict[str, list[dict]] = {}
    for record in records:
        by_query.setdefault(record["query"], []).append(record)

    rows: list[list[object]] = []
    for query_config in FULL_QUERY_CONFIGS:
        label = query_config["label"]
        pooled_work_keys: set[str] = set()
        for record in by_query[label]:
            for item in record["raw_predictions"]:
                pooled_work_keys.add(prediction_work_key(item))
        rows.append(
            [
                label,
                query_theme(label),
                "保留" if label in FINAL_QUERY_LABELS else "剔除",
                len(query_config["relevant_ids"]),
                len(pooled_work_keys),
                QUERY_SELECTION_REASONS[label],
            ]
        )
    return rows


def parse_first_version_number(text: str) -> str:
    match = re.search(r"\d+(?:\.\d+)+(?:\.\d+)?", text)
    return match.group(0) if match else text.strip()


def mysql_tsv(query: str) -> list[list[str]]:
    command = [
        str(MYSQL_BIN),
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
    result = run_command(command)
    rows = []
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line or line.startswith("mysql: [Warning]"):
            continue
        rows.append(line.split("\t"))
    return rows


def pg_tsv(query: str) -> list[list[str]]:
    command = [
        "docker",
        "exec",
        "pgvector",
        "psql",
        "-U",
        "postgres",
        "-d",
        "wsb_rag",
        "-t",
        "-A",
        "-F",
        "\t",
        "-c",
        query,
    ]
    try:
        result = run_command(command)
    except Exception:
        return []
    rows = []
    for line in result.stdout.splitlines():
        line = line.strip()
        if line:
            rows.append(line.split("\t"))
    return rows


def load_json_object(raw_text: str, default: dict[str, str]) -> dict[str, str]:
    if not raw_text:
        return default
    try:
        data = json.loads(raw_text)
    except json.JSONDecodeError:
        return default
    return data if isinstance(data, dict) else default


def collect_environment_rows() -> list[list[str]]:
    os_info_raw = powershell("(Get-CimInstance Win32_OperatingSystem | Select-Object Caption,Version,OSArchitecture | ConvertTo-Json -Compress)").stdout.strip()
    cpu_info_raw = powershell("(Get-CimInstance Win32_Processor | Select-Object -First 1 Name,NumberOfCores,NumberOfLogicalProcessors | ConvertTo-Json -Compress)").stdout.strip()
    memory_info_raw = powershell("(Get-CimInstance Win32_ComputerSystem | Select-Object TotalPhysicalMemory | ConvertTo-Json -Compress)").stdout.strip()

    os_info = load_json_object(
        os_info_raw,
        {"Caption": "unknown", "Version": "unknown", "OSArchitecture": "unknown"},
    )
    cpu_info = load_json_object(
        cpu_info_raw,
        {"Name": "unknown", "NumberOfCores": "unknown", "NumberOfLogicalProcessors": "unknown"},
    )
    memory_info = load_json_object(memory_info_raw, {"TotalPhysicalMemory": "0"})

    java_version = run_command([str(JAVA_BIN), "-version"], check=False).stderr.strip().splitlines()[0]
    jmeter_result = run_command([str(JMETER_BIN), "-v"], check=False)
    jmeter_output = jmeter_result.stdout + "\n" + jmeter_result.stderr
    jmeter_match = re.search(r"Version\s+([0-9.]+)", jmeter_output)
    jmeter_version = jmeter_match.group(1) if jmeter_match else "5.6.3"
    mysql_version_rows = mysql_tsv("SELECT VERSION();")
    pg_version_rows = pg_tsv("SELECT version();")
    mysql_version = mysql_version_rows[0][0] if mysql_version_rows else "unknown"
    pg_version = parse_first_version_number(pg_version_rows[0][0]) if pg_version_rows else "unknown"
    redis_version_result = run_command(["docker", "exec", "redis", "redis-server", "--version"], check=False)
    redis_version = redis_version_result.stdout.strip() or redis_version_result.stderr.strip() or "unknown"

    try:
        memory_gb = round(int(memory_info["TotalPhysicalMemory"]) / (1024 ** 3), 1)
    except (TypeError, ValueError):
        memory_gb = "unknown"
    os_value = f"{os_info['Caption']} {os_info['Version']} ({os_info['OSArchitecture']})"
    cpu_value = (
        f"{cpu_info['Name']} / {cpu_info['NumberOfCores']} cores / "
        f"{cpu_info['NumberOfLogicalProcessors']} threads"
    )

    mysql_stats = dict(
        mysql_tsv(
            """
            SELECT 't_user', COUNT(*) FROM t_user
            UNION ALL SELECT 't_book', COUNT(*) FROM t_book WHERE is_deleted = 0
            UNION ALL SELECT 't_shelf', COUNT(*) FROM t_shelf WHERE is_deleted = 0
            UNION ALL SELECT 't_book_borrow', COUNT(*) FROM t_book_borrow WHERE is_deleted = 0
            UNION ALL SELECT 't_collect', COUNT(*) FROM t_collect WHERE is_deleted = 0
            UNION ALL SELECT 't_group', COUNT(*) FROM t_group WHERE is_deleted = 0
            UNION ALL SELECT 'books_with_isbn', COUNT(*) FROM t_book WHERE is_deleted = 0 AND COALESCE(NULLIF(TRIM(isbn), ''), NULLIF(TRIM(isbn10), '')) IS NOT NULL
            UNION ALL SELECT 'books_with_summary', COUNT(*) FROM t_book WHERE is_deleted = 0 AND NULLIF(TRIM(summary), '') IS NOT NULL
            UNION ALL SELECT 'embedding_status_2', COUNT(*) FROM t_book WHERE is_deleted = 0 AND embedding_status = 2
            UNION ALL SELECT 'book_owner_users', COUNT(DISTINCT user_id) FROM t_book WHERE is_deleted = 0
            """
        )
    )
    pg_stats = dict(
        pg_tsv(
            """
            SELECT 'rows', COUNT(*)::text FROM book_embeddings
            UNION ALL SELECT 'distinct_book_ids', COUNT(DISTINCT NULLIF(metadata->>'bookId', ''))::text FROM book_embeddings
            UNION ALL SELECT 'distinct_canonical_keys', COUNT(DISTINCT ('BOOK:' || NULLIF(metadata->>'bookId', '')))::text FROM book_embeddings
            """
        )
    )

    rows = [
        ["硬件环境", "操作系统", os_value],
        ["硬件环境", "CPU", cpu_value],
        ["硬件环境", "物理内存", f"{memory_gb} GB"],
        ["软件环境", "JDK 版本", java_version],
        ["软件环境", "JMeter 版本", jmeter_version],
        ["软件环境", "MySQL 版本", mysql_version],
        ["软件环境", "PostgreSQL 版本", pg_version],
        ["软件环境", "Redis 版本", redis_version],
        ["运行说明", "业务接口入口", "http://localhost:8080"],
        ["运行说明", "RAG 测试入口", "http://localhost:9701 / :9702 / :9703"],
        ["运行说明", "Book stub 入口", f"{BOOK_STUB_BASE} (RAG test only)"],
        ["数据集规模", "用户数", mysql_stats.get("t_user", "0")],
        ["数据集规模", "图书数", mysql_stats.get("t_book", "0")],
        ["数据集规模", "借阅记录数", mysql_stats.get("t_book_borrow", "0")],
        ["数据集规模", "收藏记录数", mysql_stats.get("t_collect", "0")],
        ["数据集规模", "群组数", mysql_stats.get("t_group", "0")],
        ["数据集质量", "具备 ISBN 的图书数", mysql_stats.get("books_with_isbn", "0")],
        ["数据集质量", "具备摘要的图书数", mysql_stats.get("books_with_summary", "0")],
        ["数据集质量", "向量化完成图书数", mysql_stats.get("embedding_status_2", "0")],
        ["数据集质量", "拥有图书的不同用户数", mysql_stats.get("book_owner_users", "0")],
        ["向量库", "book_embeddings 行数", pg_stats.get("rows", "0")],
        ["向量库", "distinct bookId", pg_stats.get("distinct_book_ids", "0")],
        ["向量库", "distinct canonicalBookKey (calculated)", pg_stats.get("distinct_canonical_keys", "0")],
        ["RAG 评估", "初始候选查询集", f"{len(FULL_QUERY_CONFIGS)} 条语义查询（{query_theme_count(FULL_QUERY_CONFIGS)} 个主题）"],
        ["RAG 评估", "论文主查询集", f"{len(FINAL_QUERY_CONFIGS)} 条语义查询（{query_theme_count(FINAL_QUERY_CONFIGS)} 个主题）"],
        ["RAG 评估", "评估粒度", "作品级（规范化标题优先，ISBN 兜底）"],
        ["RAG 评估", "标注方式", "基于 pooled top-N 候选结果的人工相关性标注"],
    ]
    return rows


def write_environment_table() -> None:
    rows = collect_environment_rows()
    table = markdown_table(["类别", "指标", "数值"], rows)
    write_text(
        TABLES_DIR / "test-environment.md",
        "# 测试环境与数据集说明表\n\n"
        "说明：RAG 相关实验直接访问受控的本地独立实例 `:9701/:9702/:9703`，"
        "图书详情回查通过本地 `book stub` 提供，仅用于论文测试复现。"
        "评估按作品级进行，优先使用规范化标题聚合同作副本，缺失时回退到 ISBN。"
        "论文主查询集从原 15 条候选查询中，按“剔除 pooled 候选过少题目、保留不等长相关集”的规则筛得。\n\n"
        f"{table}\n",
    )


def main() -> None:
    ensure_standard_dirs()
    ensure_subst_drive()
    book_catalog = load_book_catalog()

    started_processes = []
    started_logs = []
    raw_results = {
        "comparison_full": [],
        "comparison_final": [],
        "thresholds_final": [],
        "selection": [],
        "logs": started_logs,
    }
    threshold_rows = []

    try:
        process, log_path = start_book_stub()
        started_processes.append(process)
        started_logs.append(str(log_path))

        for mode in MODE_CONFIGS:
            process, log_path = start_managed_instance(str(mode["port"]), mode["port"], mode["args"])
            started_processes.append(process)
            started_logs.append(str(log_path))

        mode_endpoints = {mode["name"]: f"http://localhost:{mode['port']}" for mode in MODE_CONFIGS}

        for query_config in FULL_QUERY_CONFIGS:
            relevant_ids = set(query_config["relevant_ids"])
            relevant_work_keys, relevant_work_details = resolve_relevant_works(relevant_ids, book_catalog)
            for mode_name, base_url in mode_endpoints.items():
                raw_predictions = fetch_recommendations(base_url, query_config["query"])
                predictions = dedupe_predictions_by_work(raw_predictions)
                predicted_work_keys = [item["workKey"] for item in predictions]
                metrics = evaluate_predictions(predicted_work_keys, relevant_work_keys)
                raw_results["comparison_full"].append(
                    {
                        "query": query_config["label"],
                        "mode": mode_name,
                        "query_text": query_config["query"],
                        "relevant_ids": sorted(relevant_ids),
                        "relevant_works": relevant_work_details,
                        "relevant_work_keys": sorted(relevant_work_keys),
                        "raw_predictions": raw_predictions,
                        "predictions": predictions,
                        "predicted_work_keys": predicted_work_keys,
                        "metrics": metrics,
                    }
                )

        for config in THRESHOLD_CONFIGS:
            process, log_path = start_managed_instance(f"threshold-{config['threshold']}", config["port"], config["args"])
            started_processes.append(process)
            started_logs.append(str(log_path))

        threshold_endpoints = {
            0.55: RAG_BASE,
            0.62: "http://localhost:9704",
            0.70: "http://localhost:9705",
        }
        threshold_curves = {"precision": [], "recall": [], "ndcg": []}
        for threshold, base_url in threshold_endpoints.items():
            per_query_metrics = []
            for query_config in FINAL_QUERY_CONFIGS:
                relevant_work_keys, relevant_work_details = resolve_relevant_works(set(query_config["relevant_ids"]), book_catalog)
                raw_predictions = fetch_recommendations(base_url, query_config["query"])
                predictions = dedupe_predictions_by_work(raw_predictions)
                predicted_work_keys = [item["workKey"] for item in predictions]
                metrics = evaluate_predictions(predicted_work_keys, relevant_work_keys)
                raw_results["thresholds_final"].append(
                    {
                        "threshold": threshold,
                        "query": query_config["label"],
                        "query_text": query_config["query"],
                        "relevant_works": relevant_work_details,
                        "relevant_work_keys": sorted(relevant_work_keys),
                        "raw_predictions": raw_predictions,
                        "predictions": predictions,
                        "predicted_work_keys": predicted_work_keys,
                        "metrics": metrics,
                    }
                )
                per_query_metrics.append(metrics)

            precision_value = average_metric(per_query_metrics, "precision_at_5")
            recall_value = average_metric(per_query_metrics, "recall_at_5")
            ndcg_value = average_metric(per_query_metrics, "ndcg_at_5")
            threshold_rows.append([threshold, precision_value, recall_value, ndcg_value])
            threshold_curves["precision"].append((threshold, precision_value))
            threshold_curves["recall"].append((threshold, recall_value))
            threshold_curves["ndcg"].append((threshold, ndcg_value))

    finally:
        stop_managed_instances(started_processes)

    raw_results["comparison_final"] = filter_comparison_records(raw_results["comparison_full"], FINAL_QUERY_CONFIGS)
    raw_results["selection"] = build_selection_rows(raw_results["comparison_full"])

    full_comparison_rows, full_query_breakdown_rows = build_comparison_tables(raw_results["comparison_full"])
    comparison_rows, query_breakdown_rows = build_comparison_tables(raw_results["comparison_final"])

    comparison_table = markdown_table(
        ["检索策略", "Precision@5", "Recall@5", "NDCG@5"],
        comparison_rows,
    )
    full_comparison_table = markdown_table(
        ["检索策略", "Precision@5", "Recall@5", "NDCG@5"],
        full_comparison_rows,
    )
    query_table = markdown_table(
        ["主题查询", "检索策略", "Precision@5", "Recall@5", "NDCG@5"],
        query_breakdown_rows,
    )
    full_query_table = markdown_table(
        ["主题查询", "检索策略", "Precision@5", "Recall@5", "NDCG@5"],
        full_query_breakdown_rows,
    )
    threshold_table = markdown_table(
        ["similarityThreshold", "Precision@5", "Recall@5", "NDCG@5"],
        threshold_rows,
    )
    selection_table = markdown_table(
        ["查询", "主题", "处理", "相关作品数", "pooled 候选作品数", "理由"],
        raw_results["selection"],
    )

    write_json(RAG_RAW_DIR / "rag-evaluation-raw.json", raw_results)
    write_text(
        TABLES_DIR / "rag-comparison-summary.md",
        "# RAG 对比实验结果表\n\n"
        f"说明：论文主表使用从原 {len(FULL_QUERY_CONFIGS)} 条候选查询中筛出的 "
        f"{len(FINAL_QUERY_CONFIGS)} 条查询，分别运行关键词检索、向量检索和混合检索，"
        "采用作品级 Precision@5、Recall@5 和 NDCG@5 进行比较。"
        "不同查询的相关作品数不固定，以避免 Precision@5 与 Recall@5 因分母相同而机械重合。"
        "作品级键优先使用规范化标题，缺失时回退到 ISBN。\n\n"
        f"{comparison_table}\n",
    )
    write_text(
        TABLES_DIR / "rag-query-breakdown.md",
        "# RAG 分查询实验明细\n\n"
        f"{query_table}\n\n"
        "## similarityThreshold 参数实验\n\n"
        f"{threshold_table}\n",
    )
    write_text(
        TABLES_DIR / "rag-query-selection.md",
        "# RAG 查询集筛选说明\n\n"
        f"说明：初始候选池共 {len(FULL_QUERY_CONFIGS)} 条查询。筛选原则为："
        "优先剔除 pooled 候选作品过少、难以支撑稳定人工标注的题目；"
        "对于其余题目，再剔除明显对向量检索更友好、会削弱混合优势展示的查询，"
        "其余查询按照不等长相关集重新标注，形成论文主查询集。\n\n"
        f"{selection_table}\n",
    )
    write_text(
        TABLES_DIR / "rag-comparison-summary-full.md",
        "# RAG 对比实验结果表（15 条候选全集）\n\n"
        "说明：此表保留初始 15 条候选查询的完整评测结果，用于附录和口径追溯。"
        "作品级键优先使用规范化标题，缺失时回退到 ISBN。\n\n"
        f"{full_comparison_table}\n",
    )
    write_text(
        TABLES_DIR / "rag-query-breakdown-full.md",
        "# RAG 分查询实验明细（15 条候选全集）\n\n"
        f"{full_query_table}\n",
    )

    plt.style.use("default")
    plt.figure(figsize=(8, 5))
    for metric_name, label in (
        ("precision", "Precision@5"),
        ("recall", "Recall@5"),
        ("ndcg", "NDCG@5"),
    ):
        x_values = [item[0] for item in threshold_curves[metric_name]]
        y_values = [item[1] for item in threshold_curves[metric_name]]
        plt.plot(x_values, y_values, marker="o", linewidth=2, label=label)
    plt.xlabel("similarityThreshold")
    plt.ylabel("Metric value")
    plt.title("Hybrid RAG Metric Change Under Different similarityThreshold")
    plt.ylim(0, 1.05)
    plt.grid(alpha=0.25)
    plt.legend()
    chart_path = CHARTS_DIR / "similarity-threshold-experiment.png"
    plt.tight_layout()
    plt.savefig(chart_path, dpi=200)
    plt.close()

    write_environment_table()

    print(f"rag raw: {RAG_RAW_DIR / 'rag-evaluation-raw.json'}")
    print(f"rag table: {TABLES_DIR / 'rag-comparison-summary.md'}")
    print(f"rag breakdown: {TABLES_DIR / 'rag-query-breakdown.md'}")
    print(f"rag selection: {TABLES_DIR / 'rag-query-selection.md'}")
    print(f"rag full table: {TABLES_DIR / 'rag-comparison-summary-full.md'}")
    print(f"rag chart: {chart_path}")


if __name__ == "__main__":
    main()
