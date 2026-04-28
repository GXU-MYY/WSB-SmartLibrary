# 功能测试汇总表

说明：仅保留论文正文中最关键的 12 条功能链路，原始响应数据见 `raw/functional/functional-results.json`。

| 测试场景       | 接口                                    | 结果   | HTTP | 响应时间(ms) | 校验点                              |
|------------|---------------------------------------|------|------|----------|----------------------------------|
| 1. 登录      | POST /v1/admin/login                  | PASS | 200  | 157.49   | 返回有效 token                       |
| 2. 我的藏书    | GET /v1/book/my                       | PASS | 200  | 40.77    | 返回个人藏书信息                         |
| 3. 图书列表分页  | GET /v1/book?page=1&page_size=10      | PASS | 200  | 33.40    | 返回分页记录且 total > 0                |
| 4. 图书详情    | GET /v1/book/detail?book_id=193       | PASS | 200  | 21.54    | 返回指定图书详情                         |
| 5. AI 摘要读取 | GET :9701/v1/rag/summary/193          | PASS | 200  | 38.95    | 返回摘要字段                           |
| 6. 相似图书推荐  | GET :9701/v1/rag/similar/5?limit=4    | PASS | 200  | 59.19    | 返回非空相似图书列表                       |
| 7. 自然语言推荐  | GET :9701/v1/rag/recommend            | PASS | 200  | 870.14   | 返回自然语言推荐结果                       |
| 8. 收藏链路    | POST/GET/DELETE /v1/collect           | PASS | 200  | 101.01   | 新增、查询、取消收藏全部成功                   |
| 9. 线下借入登记  | POST /v1/book/borrow                  | PASS | 200  | 88.32    | 创建借阅记录并返回 borrow_id              |
| 10. 借阅汇总   | GET /v1/book/borrow/summary           | PASS | 200  | 36.00    | 返回 total、active、overdue 等统计项     |
| 11. 归还图书   | POST /v1/book/returning               | PASS | 200  | 56.73    | 借阅记录状态更新为已归还                     |
| 12. 个人统计页  | GET /v1/community/statistics/personal | PASS | 200  | 141.70   | 返回 owned、borrowed、collected 三类统计 |
