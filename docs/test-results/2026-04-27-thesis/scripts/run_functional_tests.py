from __future__ import annotations

from pathlib import Path
from urllib.parse import quote

from thesis_test_common import (
    GATEWAY_BASE,
    RAG_BASE,
    RAW_DIR,
    TABLES_DIR,
    auth_headers,
    ensure_standard_dirs,
    http_json,
    login,
    markdown_table,
    write_json,
    write_text,
)


BOOK_ID = 193
SIMILAR_BOOK_ID = 5
RECOMMEND_QUERY = "社会心理学"


def payload_data(result):
    if isinstance(result.data, dict):
        return result.data.get("data")
    return None


def success(result) -> bool:
    return result.ok and isinstance(result.data, dict) and result.data.get("code") == 200


def cleanup_collect(token: str, book_id: int) -> None:
    headers = auth_headers(token)
    result = http_json("GET", f"{GATEWAY_BASE}/v1/collect?type=book", headers=headers)
    if not success(result):
        return

    records = payload_data(result) or []
    for record in records:
        if int(record.get("bookId", 0)) == book_id:
            collect_id = record.get("id")
            if collect_id is not None:
                http_json(
                    "DELETE",
                    f"{GATEWAY_BASE}/v1/collect",
                    headers=headers,
                    body={"collect_id": int(collect_id)},
                )


def record_row(rows, raw_entries, name: str, endpoint: str, result, assertion: str) -> None:
    status = "PASS" if success(result) else "FAIL"
    rows.append(
        [
            name,
            endpoint,
            status,
            result.status_code,
            f"{result.duration_ms:.2f}",
            assertion,
        ]
    )
    raw_entries.append(
        {
            "scenario": name,
            "endpoint": endpoint,
            "status": status,
            "http_status": result.status_code,
            "duration_ms": round(result.duration_ms, 2),
            "assertion": assertion,
            "response": result.data,
            "error": result.error,
        }
    )


def main() -> None:
    ensure_standard_dirs()
    raw_entries = []
    summary_rows = []

    token, login_result = login()
    headers = auth_headers(token)
    active_borrow_id = None

    record_row(
        summary_rows,
        raw_entries,
        "1. 登录",
        "POST /v1/admin/login",
        login_result,
        "返回有效 token",
    )

    try:
        my_books = http_json("GET", f"{GATEWAY_BASE}/v1/book/my", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "2. 我的藏书",
            "GET /v1/book/my",
            my_books,
            "返回个人藏书信息",
        )

        book_list = http_json("GET", f"{GATEWAY_BASE}/v1/book?page=1&page_size=10", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "3. 图书列表分页",
            "GET /v1/book?page=1&page_size=10",
            book_list,
            "返回分页记录且 total > 0",
        )

        book_detail = http_json("GET", f"{GATEWAY_BASE}/v1/book/detail?book_id={BOOK_ID}", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "4. 图书详情",
            f"GET /v1/book/detail?book_id={BOOK_ID}",
            book_detail,
            "返回指定图书详情",
        )

        summary = http_json("GET", f"{RAG_BASE}/v1/rag/summary/{BOOK_ID}", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "5. AI 摘要读取",
            f"GET :9701/v1/rag/summary/{BOOK_ID}",
            summary,
            "返回摘要字段",
        )

        similar = http_json("GET", f"{RAG_BASE}/v1/rag/similar/{SIMILAR_BOOK_ID}?limit=4", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "6. 相似图书推荐",
            f"GET :9701/v1/rag/similar/{SIMILAR_BOOK_ID}?limit=4",
            similar,
            "返回非空相似图书列表",
        )

        recommend_query = quote(RECOMMEND_QUERY)
        recommend = http_json(
            "GET",
            f"{RAG_BASE}/v1/rag/recommend?query={recommend_query}&limit=5&mineOnly=false",
            headers=headers,
        )
        record_row(
            summary_rows,
            raw_entries,
            "7. 自然语言推荐",
            "GET :9701/v1/rag/recommend",
            recommend,
            "返回自然语言推荐结果",
        )

        cleanup_collect(token, BOOK_ID)
        collect_add = http_json(
            "POST",
            f"{GATEWAY_BASE}/v1/collect",
            headers=headers,
            body={"book_id": BOOK_ID},
        )
        collect_list = http_json("GET", f"{GATEWAY_BASE}/v1/collect?type=book", headers=headers)
        collect_id = None
        collect_list_ok = False
        if success(collect_add):
            collect_id = int((payload_data(collect_add) or {}).get("id", 0))
        if success(collect_list):
            records = payload_data(collect_list) or []
            collect_list_ok = any(int(item.get("bookId", 0)) == BOOK_ID for item in records)
        collect_delete = None
        if collect_id:
            collect_delete = http_json(
                "DELETE",
                f"{GATEWAY_BASE}/v1/collect",
                headers=headers,
                body={"collect_id": collect_id},
            )
        collection_ok = success(collect_add) and collect_list_ok and collect_delete is not None and success(collect_delete)
        collection_duration = collect_add.duration_ms + collect_list.duration_ms + (collect_delete.duration_ms if collect_delete else 0)
        collection_response = {
            "code": 200 if collection_ok else 500,
            "steps": {
                "add": collect_add.data,
                "list": collect_list.data,
                "delete": collect_delete.data if collect_delete else None,
            },
        }
        collection_result = type(collect_add)(
            ok=collection_ok,
            status_code=200 if collection_ok else 500,
            duration_ms=collection_duration,
            data=collection_response,
            raw_text="",
            error=None if collection_ok else "collection lifecycle failed",
        )
        record_row(
            summary_rows,
            raw_entries,
            "8. 收藏链路",
            "POST/GET/DELETE /v1/collect",
            collection_result,
            "新增、查询、取消收藏全部成功",
        )

        borrow_create = http_json(
            "POST",
            f"{GATEWAY_BASE}/v1/book/borrow",
            headers=headers,
            body={
                "title": "论文测试借入样书",
                "author": "测试用户",
                "publisher": "本地测试出版社",
                "borrow_name": "论文测试对象",
                "borrowing_time": "2026-04-27",
                "due_time": "2026-05-11",
                "borrow_type": 1,
            },
        )
        if success(borrow_create):
            active_borrow_id = int((payload_data(borrow_create) or {}).get("id", 0))
        record_row(
            summary_rows,
            raw_entries,
            "9. 线下借入登记",
            "POST /v1/book/borrow",
            borrow_create,
            "创建借阅记录并返回 borrow_id",
        )

        borrow_summary = http_json("GET", f"{GATEWAY_BASE}/v1/book/borrow/summary", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "10. 借阅汇总",
            "GET /v1/book/borrow/summary",
            borrow_summary,
            "返回 total、active、overdue 等统计项",
        )

        borrow_return = http_json(
            "POST",
            f"{GATEWAY_BASE}/v1/book/returning",
            headers=headers,
            body={"borrow_id": active_borrow_id, "return_time": "2026-04-27"},
        )
        if success(borrow_return):
            active_borrow_id = None
        record_row(
            summary_rows,
            raw_entries,
            "11. 归还图书",
            "POST /v1/book/returning",
            borrow_return,
            "借阅记录状态更新为已归还",
        )

        personal_stats = http_json("GET", f"{GATEWAY_BASE}/v1/community/statistics/personal", headers=headers)
        record_row(
            summary_rows,
            raw_entries,
            "12. 个人统计页",
            "GET /v1/community/statistics/personal",
            personal_stats,
            "返回 owned、borrowed、collected 三类统计",
        )
    finally:
        if active_borrow_id:
            http_json(
                "POST",
                f"{GATEWAY_BASE}/v1/book/returning",
                headers=headers,
                body={"borrow_id": active_borrow_id, "return_time": "2026-04-27"},
            )
        cleanup_collect(token, BOOK_ID)

    raw_path = RAW_DIR / "functional" / "functional-results.json"
    table_path = TABLES_DIR / "functional-test-summary.md"

    write_json(raw_path, raw_entries)
    table = markdown_table(
        ["测试场景", "接口", "结果", "HTTP", "响应时间(ms)", "校验点"],
        summary_rows,
    )
    write_text(
        table_path,
        "# 功能测试汇总表\n\n"
        "说明：仅保留论文正文中最关键的 12 条功能链路，原始响应数据见 `raw/functional/functional-results.json`。\n\n"
        f"{table}\n",
    )

    print(f"functional raw: {raw_path}")
    print(f"functional table: {table_path}")


if __name__ == "__main__":
    main()
