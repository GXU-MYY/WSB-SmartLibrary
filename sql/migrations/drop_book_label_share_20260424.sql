-- ==========================================
-- 删除未使用的 t_book_label / t_share 表
-- 执行前请确认当前环境不再依赖这两条业务链路
-- ==========================================

DROP TABLE IF EXISTS `t_book_label`;
DROP TABLE IF EXISTS `t_share`;
