ALTER TABLE `t_book`
  ADD COLUMN `is_lent_out` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否借出中' AFTER `is_borrowed`;

ALTER TABLE `t_book_borrow`
  ADD COLUMN `borrow_flow_id` varchar(64) DEFAULT NULL COMMENT '借阅流ID' AFTER `cover_url`,
  ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT '群组ID' AFTER `borrow_flow_id`,
  ADD COLUMN `request_id` bigint DEFAULT NULL COMMENT '借阅申请ID' AFTER `group_id`,
  ADD INDEX `idx_borrow_flow_id` (`borrow_flow_id`) USING BTREE,
  ADD INDEX `idx_group_id` (`group_id`) USING BTREE,
  ADD INDEX `idx_request_id` (`request_id`) USING BTREE;

CREATE TABLE IF NOT EXISTS `t_group_borrow_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群组ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `book_name` varchar(255) NOT NULL COMMENT '图书名称快照',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面快照',
  `shelf_id` bigint DEFAULT NULL COMMENT '来源书架ID',
  `shelf_name` varchar(100) DEFAULT NULL COMMENT '来源书架名称',
  `owner_user_id` bigint NOT NULL COMMENT '出借人用户ID',
  `owner_nickname` varchar(50) DEFAULT NULL COMMENT '出借人昵称快照',
  `borrower_user_id` bigint NOT NULL COMMENT '借入人用户ID',
  `borrower_nickname` varchar(50) DEFAULT NULL COMMENT '借入人昵称快照',
  `due_time` date DEFAULT NULL COMMENT '预计归还日期',
  `request_remark` varchar(500) DEFAULT NULL COMMENT '申请备注',
  `borrow_flow_id` varchar(64) DEFAULT NULL COMMENT '借阅流ID',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '申请状态：0-待处理，1-已同意，2-已拒绝',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_owner_user_id` (`owner_user_id`) USING BTREE,
  KEY `idx_borrower_user_id` (`borrower_user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_borrow_flow_id` (`borrow_flow_id`) USING BTREE
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组借阅申请表';

UPDATE `t_book` b
SET `is_lent_out` = EXISTS (
  SELECT 1
  FROM `t_book_borrow` bb
  WHERE bb.`book_id` = b.`id`
    AND bb.`borrow_type` = 2
    AND bb.`is_deleted` = 0
    AND bb.`status` IN (0, 2)
);
