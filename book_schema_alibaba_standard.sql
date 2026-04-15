-- ==========================================
-- 微书包数据库设计（当前交付版）
-- 说明：
-- 1. 仅保留当前仍在使用的业务表
-- 2. 评论与评论点赞能力已移除，不再包含 t_comment / t_comment_like
-- 3. 收藏能力已迁移到图书域，仅支持图书与书架收藏
-- ==========================================

-- ==========================================
-- 用户表：t_user
-- ==========================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(255) NOT NULL COMMENT '密码（加密存储）',
  `is_active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否激活：0-未激活，1-已激活',
  `is_confirmed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否完成确认：0-未确认，1-已确认',
  `remember_token` varchar(255) DEFAULT NULL COMMENT '记住登录 Token',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `nick_name` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像 URL',
  `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_name` (`user_name`) USING BTREE,
  KEY `idx_phone` (`phone`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==========================================
-- 图书表：t_book
-- ==========================================
DROP TABLE IF EXISTS `t_book`;
CREATE TABLE `t_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '书名',
  `subtitle` varchar(255) DEFAULT NULL COMMENT '副标题',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面 URL',
  `author` varchar(255) DEFAULT NULL COMMENT '作者',
  `summary` text DEFAULT NULL COMMENT '内容简介',
  `embedding_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '向量状态：0-未处理，1-处理中，2-已完成',
  `publisher` varchar(255) DEFAULT NULL COMMENT '出版社',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `page_count` int DEFAULT NULL COMMENT '页数',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `binding` varchar(50) DEFAULT NULL COMMENT '装帧',
  `isbn` varchar(20) DEFAULT NULL COMMENT 'ISBN-13',
  `isbn10` varchar(13) DEFAULT NULL COMMENT 'ISBN-10',
  `keyword` varchar(500) DEFAULT NULL COMMENT '关键词',
  `edition` varchar(50) DEFAULT NULL COMMENT '版次',
  `impression` varchar(50) DEFAULT NULL COMMENT '印次',
  `language` varchar(20) DEFAULT NULL COMMENT '语言',
  `book_format` varchar(50) DEFAULT NULL COMMENT '开本',
  `classify` varchar(100) DEFAULT '其他' COMMENT '自定义分类',
  `cip` varchar(100) DEFAULT NULL COMMENT 'CIP',
  `clc` varchar(100) DEFAULT '其他' COMMENT '中图法分类',
  `label` varchar(500) DEFAULT NULL COMMENT '标签',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `is_on_shelf` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在架：0-否，1-是',
  `is_borrowed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为借入书：0-否，1-是',
  `is_lent_out` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否借出中：0-否，1-是',
  `user_id` bigint NOT NULL COMMENT '所有者用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_isbn` (`isbn`) USING BTREE,
  KEY `idx_title` (`title`) USING BTREE,
  KEY `idx_author` (`author`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_is_deleted` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

-- ==========================================
-- 书架表：t_shelf
-- ==========================================
DROP TABLE IF EXISTS `t_shelf`;
CREATE TABLE `t_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shelf_name` varchar(100) NOT NULL COMMENT '书架名称',
  `address` varchar(255) DEFAULT NULL COMMENT '书架位置',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开：0-否，1-是',
  `shelf_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '类型：1-实体书架，2-虚拟书单',
  `remark` varchar(500) DEFAULT NULL COMMENT '书架描述',
  `user_id` bigint NOT NULL COMMENT '所有者用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_shelf_type` (`shelf_type`) USING BTREE,
  KEY `idx_is_public` (`is_public`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书架表';

-- ==========================================
-- 图书-书架关联表：t_book_shelf
-- ==========================================
DROP TABLE IF EXISTS `t_book_shelf`;
CREATE TABLE `t_book_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shelf_id` bigint NOT NULL COMMENT '书架ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_shelf_book` (`shelf_id`, `book_id`) USING BTREE,
  KEY `idx_shelf_id` (`shelf_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书-书架关联表';

-- ==========================================
-- 图书标签表：t_book_label
-- ==========================================
DROP TABLE IF EXISTS `t_book_label`;
CREATE TABLE `t_book_label` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `label` varchar(50) NOT NULL COMMENT '标签名',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_label` (`label`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书标签表';

-- ==========================================
-- 阅读记录表：t_book_reading
-- ==========================================
DROP TABLE IF EXISTS `t_book_reading`;
CREATE TABLE `t_book_reading` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `reading_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '阅读状态：1-想读，2-在读，3-已读',
  `book_name` varchar(255) DEFAULT NULL COMMENT '书名快照',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面快照',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_reading_status` (`reading_status`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读记录表';

-- ==========================================
-- 借阅记录表：t_book_borrow
-- ==========================================
DROP TABLE IF EXISTS `t_book_borrow`;
CREATE TABLE `t_book_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `user_id` bigint NOT NULL COMMENT '记录所属用户ID',
  `borrower_name` varchar(50) NOT NULL COMMENT '借阅对方姓名',
  `borrow_time` date NOT NULL COMMENT '借阅日期',
  `due_time` date DEFAULT NULL COMMENT '预计归还日期',
  `return_time` date DEFAULT NULL COMMENT '实际归还日期',
  `borrow_type` tinyint(1) NOT NULL COMMENT '借阅方向：1-借入，2-借出',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '借阅状态：0-借阅中，1-已归还，2-已逾期',
  `book_name` varchar(255) DEFAULT NULL COMMENT '图书名称快照',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面快照',
  `borrow_flow_id` varchar(64) DEFAULT NULL COMMENT '借阅流ID',
  `group_id` bigint DEFAULT NULL COMMENT '群组ID',
  `request_id` bigint DEFAULT NULL COMMENT '借阅申请ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_due_time` (`due_time`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_borrow_type` (`borrow_type`) USING BTREE,
  KEY `idx_borrow_flow_id` (`borrow_flow_id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_request_id` (`request_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ==========================================
-- 收藏表：t_collect
-- ==========================================
DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `target_id` bigint NOT NULL COMMENT '收藏目标ID',
  `collect_type` tinyint(1) NOT NULL COMMENT '收藏类型：1-图书，2-书架',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `collect_type`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_target_id` (`target_id`) USING BTREE,
  KEY `idx_collect_type` (`collect_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ==========================================
-- 群组表：t_group
-- ==========================================
DROP TABLE IF EXISTS `t_group`;
CREATE TABLE `t_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_name` varchar(100) NOT NULL COMMENT '群组名称',
  `owner_id` bigint NOT NULL COMMENT '群主用户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '群组描述',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_owner_id` (`owner_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ==========================================
-- 群组成员表：t_group_user
-- ==========================================
DROP TABLE IF EXISTS `t_group_user`;
CREATE TABLE `t_group_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- ==========================================
-- 分享表：t_share
-- ==========================================
DROP TABLE IF EXISTS `t_share`;
CREATE TABLE `t_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群组ID',
  `target_id` bigint NOT NULL COMMENT '分享目标ID',
  `share_type` tinyint(1) NOT NULL COMMENT '分享类型：1-图书，2-书架',
  `share_user_id` bigint NOT NULL COMMENT '分享用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_share_user_id` (`share_user_id`) USING BTREE,
  KEY `idx_share_type` (`share_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享表';

-- ==========================================
-- 群组借阅申请表：t_group_borrow_request
-- ==========================================
DROP TABLE IF EXISTS `t_group_borrow_request`;
CREATE TABLE `t_group_borrow_request` (
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
