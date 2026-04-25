# SQL Files

`sql/` 目录按用途拆分：

- `sql/schema/`
  完整建表脚本或标准化库结构脚本。
- `sql/migrations/`
  增量变更、阶段性修复或模块清理脚本。
- `sql/rag/`
  RAG / pgvector 相关初始化脚本。

当前文件分布：

- `sql/schema/book_schema_alibaba_standard.sql`
- `sql/migrations/community_borrow_phase2_20260413.sql`
- `sql/migrations/social_module_cleanup_20260414.sql`
- `sql/rag/rag_pgvector_init.sql`
