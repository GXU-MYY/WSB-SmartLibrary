-- ==========================================
-- 下线 wsb-social 模块后的数据库清理脚本
-- 说明：
-- 1. 评论与评论点赞能力已移除
-- 2. 收藏能力保留，但仅支持图书和书架收藏
-- ==========================================

DELETE FROM `t_collect`
WHERE `collect_type` = 3;

DROP TABLE IF EXISTS `t_comment_like`;
DROP TABLE IF EXISTS `t_comment`;

ALTER TABLE `t_collect`
  MODIFY COLUMN `collect_type` tinyint(1) NOT NULL COMMENT '收藏类型：1-图书，2-书架';
