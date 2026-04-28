# 2026-04-27 Thesis Test Results

本目录用于保存毕业论文测试部分的原始数据、汇总表格和图表。

## Structure

- `scripts/`
  - `thesis_test_common.py`: 通用 HTTP、文件和命令工具
  - `run_functional_tests.py`: 功能测试与功能汇总表
  - `run_performance_tests.py`: JMeter 压测与性能汇总表
  - `run_rag_eval.py`: RAG 对比实验、阈值实验、环境说明表
- `jmeter/`
  - `generic-get.jmx`: 通用 GET 压测模板
- `raw/`
  - `functional/`: 功能测试原始 JSON
  - `performance/`: JMeter JTL、日志、性能原始 JSON
  - `rag/`: RAG 实验原始 JSON、临时日志
- `tables/`
  - `test-environment.md`
  - `functional-test-summary.md`
  - `performance-test-summary.md`
  - `rag-comparison-summary.md`
  - `rag-query-breakdown.md`
- `charts/`
  - `similarity-threshold-experiment.png`

## Notes

- 普通业务接口通过网关 `http://localhost:8080` 测试。
- RAG 相关接口直接访问修复后的独立实例 `http://localhost:9701`，避免旧实例混入结果。
- 压测指标使用平均响应时间、P95、吞吐量和错误率。
- RAG 评估采用 5 个主题查询，并基于 pooled top-5 结果做人工相关性标注。
