from __future__ import annotations

import csv
from pathlib import Path

from thesis_test_common import (
    JMETER_BIN,
    JMETER_DIR,
    RAW_DIR,
    TABLES_DIR,
    auth_headers,
    ensure_standard_dirs,
    login,
    markdown_table,
    percentile,
    run_command,
    write_json,
    write_text,
)


TEST_PLAN = JMETER_DIR / "generic-get.jmx"
PERF_RAW_DIR = RAW_DIR / "performance"

SCENARIOS = [
    {
        "slug": "book_list",
        "name": "图书列表",
        "path": "/v1/book?page=1&page_size=10",
        "port": 8080,
        "threads": [10, 20, 50],
        "duration": 30,
        "ramp_up": 8,
    },
    {
        "slug": "book_detail",
        "name": "图书详情",
        "path": "/v1/book/detail?book_id=193",
        "port": 8080,
        "threads": [10, 20, 50],
        "duration": 30,
        "ramp_up": 8,
    },
    {
        "slug": "rag_recommend",
        "name": "RAG 自然语言推荐",
        "path": "/v1/rag/recommend?query=%E7%A4%BE%E4%BC%9A%E5%BF%83%E7%90%86%E5%AD%A6&limit=5&mineOnly=false",
        "port": 9701,
        "threads": [5, 10, 20],
        "duration": 30,
        "ramp_up": 5,
    },
    {
        "slug": "rag_similar",
        "name": "RAG 相似图书",
        "path": "/v1/rag/similar/5?limit=4",
        "port": 9701,
        "threads": [5, 10, 20],
        "duration": 30,
        "ramp_up": 5,
    },
]


def parse_jtl(jtl_path: Path, fallback_window_seconds: int) -> dict[str, float]:
    elapsed_values = []
    timestamps = []
    success_count = 0
    total = 0

    with jtl_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            total += 1
            elapsed = float(row.get("elapsed") or row.get("time") or 0.0)
            elapsed_values.append(elapsed)
            timestamps.append(float(row.get("timeStamp") or 0.0))
            if str(row.get("success")).lower() == "true":
                success_count += 1

    observed_window_seconds = fallback_window_seconds
    if len(timestamps) >= 2:
        observed_window_seconds = max((max(timestamps) - min(timestamps)) / 1000.0, 1.0)

    throughput = total / observed_window_seconds if observed_window_seconds else 0.0
    error_rate = ((total - success_count) / total) * 100 if total else 0.0
    avg_ms = sum(elapsed_values) / total if total else 0.0
    p95_ms = percentile(elapsed_values, 0.95)

    return {
        "samples": total,
        "success_count": success_count,
        "avg_ms": round(avg_ms, 2),
        "p95_ms": round(p95_ms, 2),
        "throughput_rps": round(throughput, 2),
        "error_rate_pct": round(error_rate, 2),
    }


def build_properties_file(token: str, scenario: dict, threads: int, properties_path: Path) -> None:
    properties = {
        "threads": str(threads),
        "ramp_up": str(scenario["ramp_up"]),
        "duration": str(scenario["duration"]),
        "host": "localhost",
        "port": str(scenario["port"]),
        "protocol": "http",
        "path": scenario["path"],
        "token": token,
        "connect_timeout": "10000",
        "response_timeout": "30000",
        "jmeter.save.saveservice.output_format": "csv",
        "jmeter.save.saveservice.print_field_names": "true",
        "jmeter.save.saveservice.timestamp_format": "ms",
        "jmeter.save.saveservice.time": "true",
        "jmeter.save.saveservice.label": "true",
        "jmeter.save.saveservice.successful": "true",
        "jmeter.save.saveservice.thread_name": "true",
        "jmeter.save.saveservice.response_code": "true",
        "jmeter.save.saveservice.response_message": "true",
        "jmeter.save.saveservice.data_type": "true",
        "jmeter.save.saveservice.bytes": "true",
        "jmeter.save.saveservice.sent_bytes": "true",
        "jmeter.save.saveservice.connect_time": "true",
        "jmeter.save.saveservice.latency": "true",
    }
    properties_text = "\n".join(f"{key}={value}" for key, value in properties.items()) + "\n"
    properties_path.write_text(properties_text, encoding="utf-8")


def build_jmeter_command(properties_path: Path, jtl_path: Path, log_path: Path) -> list[str]:
    return [
        str(JMETER_BIN),
        "-n",
        "-t",
        str(TEST_PLAN),
        "-q",
        str(properties_path),
        "-l",
        str(jtl_path),
        "-j",
        str(log_path),
    ]


def main() -> None:
    ensure_standard_dirs()
    token, _ = login()
    raw_results = []
    summary_rows = []

    for scenario in SCENARIOS:
        for threads in scenario["threads"]:
            base_name = f"{scenario['slug']}_{threads}"
            jtl_path = PERF_RAW_DIR / f"{base_name}.csv"
            log_path = PERF_RAW_DIR / f"{base_name}.log"
            properties_path = PERF_RAW_DIR / f"{base_name}.properties"
            if jtl_path.exists():
                jtl_path.unlink()
            if log_path.exists():
                log_path.unlink()
            if properties_path.exists():
                properties_path.unlink()

            build_properties_file(token, scenario, threads, properties_path)
            command = build_jmeter_command(properties_path, jtl_path, log_path)
            result = run_command(command, timeout=scenario["duration"] + 180)
            metrics = parse_jtl(jtl_path, scenario["duration"])

            record = {
                "scenario": scenario["name"],
                "slug": scenario["slug"],
                "path": scenario["path"],
                "threads": threads,
                "duration_seconds": scenario["duration"],
                "port": scenario["port"],
                "jmeter_stdout": result.stdout[-4000:],
                "jmeter_stderr": result.stderr[-4000:],
                "jtl_file": str(jtl_path),
                "log_file": str(log_path),
                "properties_file": str(properties_path),
                "metrics": metrics,
            }
            raw_results.append(record)
            summary_rows.append(
                [
                    scenario["name"],
                    threads,
                    metrics["samples"],
                    metrics["avg_ms"],
                    metrics["p95_ms"],
                    metrics["throughput_rps"],
                    metrics["error_rate_pct"],
                ]
            )

    raw_path = PERF_RAW_DIR / "performance-summary.json"
    table_path = TABLES_DIR / "performance-test-summary.md"

    write_json(raw_path, raw_results)
    table = markdown_table(
        ["接口场景", "并发数", "样本数", "平均响应(ms)", "P95(ms)", "吞吐量(req/s)", "错误率(%)"],
        summary_rows,
    )
    write_text(
        table_path,
        "# 核心接口性能测试表\n\n"
        "说明：使用 JMeter 5.6.3 对 4 类核心接口进行压测，正文保留平均响应时间、P95、吞吐量与错误率。\n\n"
        f"{table}\n",
    )

    print(f"performance raw: {raw_path}")
    print(f"performance table: {table_path}")


if __name__ == "__main__":
    main()
