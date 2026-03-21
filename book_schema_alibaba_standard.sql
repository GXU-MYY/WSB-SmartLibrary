-- ==========================================
-- 微书包 - 符合阿里巴巴Java开发规范的数据库设计
-- ==========================================

-- ==========================================
-- 核心表：用户表 (t_user)
-- ==========================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name` varchar(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `is_active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否激活：0-未激活，1-已激活',
  `is_confirmed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否验证：0-未验证，1-已验证',
  `remember_token` varchar(255) DEFAULT NULL COMMENT '记住登录token',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号码',
  `nick_name` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱地址',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
  `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_name` (`user_name`) USING BTREE COMMENT '用户名唯一',
  KEY `idx_phone` (`phone`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==========================================
-- 核心表：图书表 (t_book)
-- ==========================================
DROP TABLE IF EXISTS `t_book`;
CREATE TABLE `t_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '书名（主标题）',
  `subtitle` varchar(255) DEFAULT NULL COMMENT '副标题',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `author` varchar(255) DEFAULT NULL COMMENT '作者（多个作者用逗号分隔）',
  `summary` text DEFAULT NULL COMMENT '内容简介',
  `embedding_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '向量状态：0-未处理，1-处理中，2-已完成',
  `publisher` varchar(255) DEFAULT NULL COMMENT '出版社',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `page_count` int DEFAULT NULL COMMENT '页数',
  `price` decimal(10,2) DEFAULT NULL COMMENT '定价（单位：元）',
  `binding` varchar(50) DEFAULT NULL COMMENT '装帧方式（平装/精装）',
  `isbn` varchar(20) DEFAULT NULL COMMENT 'ISBN-13书号',
  `isbn10` varchar(13) DEFAULT NULL COMMENT 'ISBN-10书号',
  `keyword` varchar(500) DEFAULT NULL COMMENT '关键词（多个用逗号分隔）',
  `edition` varchar(50) DEFAULT NULL COMMENT '版次',
  `impression` varchar(50) DEFAULT NULL COMMENT '印次',
  `language` varchar(20) DEFAULT NULL COMMENT '语言（中文/英文）',
  `book_format` varchar(50) DEFAULT NULL COMMENT '开本（16开/32开）',
  `classify` varchar(100) DEFAULT '其他' COMMENT '用户自定义分类',
  `cip` varchar(100) DEFAULT NULL COMMENT 'CIP核字号',
  `clc` varchar(100) DEFAULT '其他' COMMENT '中图法分类（如I247.5）',
  `label` varchar(500) DEFAULT NULL COMMENT '书籍标签（多个用逗号分隔）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注信息',
  `is_on_shelf` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在架：0-不在架，1-在架',
  `is_borrowed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为借来的书：0-自己的书，1-借入的书',
  `user_id` bigint NOT NULL COMMENT '所有者用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_isbn` (`isbn`) USING BTREE,
  KEY `idx_title` (`title`) USING BTREE,
  KEY `idx_author` (`author`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_is_deleted` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书信息表';

-- ==========================================
-- 关联表：书架/书单表 (t_shelf)
-- ==========================================
DROP TABLE IF EXISTS `t_shelf`;
CREATE TABLE `t_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shelf_name` varchar(100) NOT NULL COMMENT '书架/书单名称',
  `address` varchar(255) DEFAULT NULL COMMENT '书架物理位置（如：卧室、书房）',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开：0-私有，1-公开',
  `shelf_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '类型：1-实体书架，2-虚拟书单',
  `remark` varchar(500) DEFAULT NULL COMMENT '书架描述',
  `user_id` bigint NOT NULL COMMENT '所有者用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_shelf_type` (`shelf_type`) USING BTREE,
  KEY `idx_is_public` (`is_public`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书架/书单表';

-- ==========================================
-- 关联表：图书-书架关联表 (t_book_shelf)
-- ==========================================
DROP TABLE IF EXISTS `t_book_shelf`;
CREATE TABLE `t_book_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shelf_id` bigint NOT NULL COMMENT '书架ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_shelf_book` (`shelf_id`, `book_id`) USING BTREE COMMENT '同一本书不能重复加入同一书架',
  KEY `idx_shelf_id` (`shelf_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书-书架关联表';

-- ==========================================
-- 业务表：图书借阅表 (t_book_borrow)
-- ==========================================
DROP TABLE IF EXISTS `t_book_borrow`;
CREATE TABLE `t_book_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `borrower_name` varchar(50) NOT NULL COMMENT '借阅对方姓名',
  `borrow_time` date NOT NULL COMMENT '借阅日期',
  `return_time` date DEFAULT NULL COMMENT '归还日期（NULL表示未归还）',
  `borrow_type` tinyint(1) NOT NULL COMMENT '借阅方向：1-借入，2-借出',
  `book_name` varchar(255) DEFAULT NULL COMMENT '图书名称（冗余）',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面URL（冗余）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_return_time` (`return_time`) USING BTREE,
  KEY `idx_borrow_type` (`borrow_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书借阅记录表';

-- ==========================================
-- 业务表：图书阅读记录表 (t_book_reading)
-- ==========================================
DROP TABLE IF EXISTS `t_book_reading`;
CREATE TABLE `t_book_reading` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `reading_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '阅读状态：1-想读，2-在读，3-已读',
  `book_name` varchar(255) DEFAULT NULL COMMENT '书名冗余（避免JOIN查询）',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面URL冗余',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`) USING BTREE COMMENT '每本书每个用户只保留一条记录',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_reading_status` (`reading_status`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书阅读记录表';

-- ==========================================
-- 标签表：图书标签表 (t_book_label)
-- ==========================================
DROP TABLE IF EXISTS `t_book_label`;
CREATE TABLE `t_book_label` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `label` varchar(50) NOT NULL COMMENT '标签名称',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_label` (`label`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书标签表';

-- ==========================================
-- 评论表 (t_comment)
-- ==========================================
DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `book_id` bigint NOT NULL COMMENT '图书ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `user_nickname` varchar(50) DEFAULT NULL COMMENT '评论者昵称（冗余）',
  `user_avatar` varchar(255) DEFAULT NULL COMMENT '评论者头像（冗余）',
  `content` varchar(1000) NOT NULL COMMENT '评论内容',
  `star_rating` tinyint(1) DEFAULT NULL COMMENT '评分：1-5星',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_star_rating` (`star_rating`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书评论表';

-- ==========================================
-- 评论点赞表 (t_comment_like)
-- ==========================================
DROP TABLE IF EXISTS `t_comment_like`;
CREATE TABLE `t_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`) USING BTREE COMMENT '防止重复点赞',
  KEY `idx_comment_id` (`comment_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- ==========================================
-- 收藏表 (t_collect)
-- ==========================================
DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `target_id` bigint NOT NULL COMMENT '收藏目标ID',
  `collect_type` tinyint(1) NOT NULL COMMENT '收藏类型：1-图书，2-书架，3-评论',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `collect_type`) USING BTREE COMMENT '防止重复收藏',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_target_id` (`target_id`) USING BTREE,
  KEY `idx_collect_type` (`collect_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ==========================================
-- 群组表 (t_group)
-- ==========================================
DROP TABLE IF EXISTS `t_group`;
CREATE TABLE `t_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_name` varchar(100) NOT NULL COMMENT '群组名称',
  `owner_id` bigint NOT NULL COMMENT '群主用户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '群组描述',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_owner_id` (`owner_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ==========================================
-- 群组成员表 (t_group_user)
-- ==========================================
DROP TABLE IF EXISTS `t_group_user`;
CREATE TABLE `t_group_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`) USING BTREE COMMENT '防止重复加入',
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- ==========================================
-- 分享表 (t_share)
-- ==========================================
DROP TABLE IF EXISTS `t_share`;
CREATE TABLE `t_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '目标群组ID',
  `target_id` bigint NOT NULL COMMENT '分享目标ID（book_id或shelf_id）',
  `share_type` tinyint(1) NOT NULL COMMENT '分享类型：1-图书，2-书架',
  `share_user_id` bigint NOT NULL COMMENT '分享人用户ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_share_user_id` (`share_user_id`) USING BTREE,
  KEY `idx_share_type` (`share_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享表';
