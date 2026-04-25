-- ==========================================
-- 寰功鍖呮暟鎹簱璁捐锛堝綋鍓嶄氦浠樼増锛?-- 璇存槑锛?-- 1. 浠呬繚鐣欏綋鍓嶄粛鍦ㄤ娇鐢ㄧ殑涓氬姟琛?-- 2. 璇勮涓庤瘎璁虹偣璧炶兘鍔涘凡绉婚櫎锛屼笉鍐嶅寘鍚?t_comment / t_comment_like
-- 3. 鏀惰棌鑳藉姏宸茶縼绉诲埌鍥句功鍩燂紝浠呮敮鎸佸浘涔︿笌涔︽灦鏀惰棌
-- ==========================================

-- ==========================================
-- 鐢ㄦ埛琛細t_user
-- ==========================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_name` varchar(50) NOT NULL COMMENT '鐧诲綍璐﹀彿',
  `password` varchar(255) NOT NULL COMMENT '瀵嗙爜锛堝姞瀵嗗瓨鍌級',
  `is_active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁婵€娲伙細0-鏈縺娲伙紝1-宸叉縺娲?,
  `is_confirmed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁瀹屾垚纭锛?-鏈‘璁わ紝1-宸茬‘璁?,
  `remember_token` varchar(255) DEFAULT NULL COMMENT '璁颁綇鐧诲綍 Token',
  `real_name` varchar(50) DEFAULT NULL COMMENT '鐪熷疄濮撳悕',
  `id_card` varchar(18) DEFAULT NULL COMMENT '韬唤璇佸彿',
  `phone` varchar(11) DEFAULT NULL COMMENT '鎵嬫満鍙?,
  `nick_name` varchar(50) DEFAULT NULL COMMENT '鏄电О',
  `email` varchar(100) DEFAULT NULL COMMENT '閭',
  `avatar` varchar(255) DEFAULT NULL COMMENT '澶村儚 URL',
  `signature` varchar(255) DEFAULT NULL COMMENT '涓€х鍚?,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_name` (`user_name`) USING BTREE,
  KEY `idx_phone` (`phone`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛琛?;

-- ==========================================
-- 鍥句功琛細t_book
-- ==========================================
DROP TABLE IF EXISTS `t_book`;
CREATE TABLE `t_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `title` varchar(255) NOT NULL COMMENT '涔﹀悕',
  `subtitle` varchar(255) DEFAULT NULL COMMENT '鍓爣棰?,
  `cover_url` varchar(500) DEFAULT NULL COMMENT '灏侀潰 URL',
  `author` varchar(255) DEFAULT NULL COMMENT '浣滆€?,
  `summary` text DEFAULT NULL COMMENT '鍐呭绠€浠?,
  `embedding_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鍚戦噺鐘舵€侊細0-寰呭鐞嗭紝1-澶勭悊涓紝2-宸插畬鎴?,
  `publisher` varchar(255) DEFAULT NULL COMMENT '鍑虹増绀?,
  `publish_date` date DEFAULT NULL COMMENT '鍑虹増鏃ユ湡',
  `page_count` int DEFAULT NULL COMMENT '椤垫暟',
  `price` decimal(10,2) DEFAULT NULL COMMENT '浠锋牸',
  `binding` varchar(50) DEFAULT NULL COMMENT '瑁呭抚',
  `isbn` varchar(20) DEFAULT NULL COMMENT 'ISBN-13',
  `isbn10` varchar(13) DEFAULT NULL COMMENT 'ISBN-10',
  `keyword` varchar(500) DEFAULT NULL COMMENT '鍏抽敭璇?,
  `edition` varchar(50) DEFAULT NULL COMMENT '鐗堟',
  `impression` varchar(50) DEFAULT NULL COMMENT '鍗版',
  `language` varchar(20) DEFAULT NULL COMMENT '璇█',
  `book_format` varchar(50) DEFAULT NULL COMMENT '寮€鏈?,
  `classify` varchar(100) DEFAULT '鍏朵粬' COMMENT '鑷畾涔夊垎绫?,
  `cip` varchar(100) DEFAULT NULL COMMENT 'CIP',
  `clc` varchar(100) DEFAULT '鍏朵粬' COMMENT '涓浘娉曞垎绫?,
  `label` varchar(500) DEFAULT NULL COMMENT '鏍囩',
  `remark` varchar(500) DEFAULT NULL COMMENT '澶囨敞',
  `is_on_shelf` tinyint(1) NOT NULL DEFAULT 1 COMMENT '鏄惁鍦ㄦ灦锛?-鍚︼紝1-鏄?,
  `is_borrowed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁涓哄€熷叆涔︼細0-鍚︼紝1-鏄?,
  `is_lent_out` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍊熷嚭涓細0-鍚︼紝1-鏄?,
  `user_id` bigint NOT NULL COMMENT '鎵€鏈夎€呯敤鎴稩D',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_isbn` (`isbn`) USING BTREE,
  KEY `idx_title` (`title`) USING BTREE,
  KEY `idx_author` (`author`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_is_deleted` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍥句功琛?;

-- ==========================================
-- 涔︽灦琛細t_shelf
-- ==========================================
DROP TABLE IF EXISTS `t_shelf`;
CREATE TABLE `t_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `shelf_name` varchar(100) NOT NULL COMMENT '涔︽灦鍚嶇О',
  `address` varchar(255) DEFAULT NULL COMMENT '涔︽灦浣嶇疆',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍏紑锛?-鍚︼紝1-鏄?,
  `shelf_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '绫诲瀷锛?-瀹炰綋涔︽灦锛?-铏氭嫙涔﹀崟',
  `remark` varchar(500) DEFAULT NULL COMMENT '涔︽灦鎻忚堪',
  `user_id` bigint NOT NULL COMMENT '鎵€鏈夎€呯敤鎴稩D',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_shelf_type` (`shelf_type`) USING BTREE,
  KEY `idx_is_public` (`is_public`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='涔︽灦琛?;

-- ==========================================
-- 鍥句功-涔︽灦鍏宠仈琛細t_book_shelf
-- ==========================================
DROP TABLE IF EXISTS `t_book_shelf`;
CREATE TABLE `t_book_shelf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `shelf_id` bigint NOT NULL COMMENT '涔︽灦ID',
  `book_id` bigint NOT NULL COMMENT '鍥句功ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_shelf_book` (`shelf_id`, `book_id`) USING BTREE,
  KEY `idx_shelf_id` (`shelf_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍥句功-涔︽灦鍏宠仈琛?;


-- ==========================================
-- 闃呰璁板綍琛細t_book_reading
-- ==========================================
DROP TABLE IF EXISTS `t_book_reading`;
CREATE TABLE `t_book_reading` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `book_id` bigint NOT NULL COMMENT '鍥句功ID',
  `reading_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '闃呰鐘舵€侊細1-鎯宠锛?-鍦ㄨ锛?-宸茶',
  `book_name` varchar(255) DEFAULT NULL COMMENT '涔﹀悕蹇収',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '灏侀潰蹇収',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_reading_status` (`reading_status`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闃呰璁板綍琛?;

-- ==========================================
-- 鍊熼槄璁板綍琛細t_book_borrow
-- ==========================================
DROP TABLE IF EXISTS `t_book_borrow`;
CREATE TABLE `t_book_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `book_id` bigint NOT NULL COMMENT '鍥句功ID',
  `user_id` bigint NOT NULL COMMENT '璁板綍鎵€灞炵敤鎴稩D',
  `borrower_name` varchar(50) NOT NULL COMMENT '鍊熼槄瀵规柟濮撳悕',
  `borrow_time` date NOT NULL COMMENT '鍊熼槄鏃ユ湡',
  `due_time` date DEFAULT NULL COMMENT '棰勮褰掕繕鏃ユ湡',
  `return_time` date DEFAULT NULL COMMENT '瀹為檯褰掕繕鏃ユ湡',
  `borrow_type` tinyint(1) NOT NULL COMMENT '鍊熼槄鏂瑰悜锛?-鍊熷叆锛?-鍊熷嚭',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鍊熼槄鐘舵€侊細0-鍊熼槄涓紝1-宸插綊杩橈紝2-宸查€炬湡',
  `book_name` varchar(255) DEFAULT NULL COMMENT '鍥句功鍚嶇О蹇収',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '灏侀潰蹇収',
  `borrow_flow_id` varchar(64) DEFAULT NULL COMMENT '鍊熼槄娴両D',
  `group_id` bigint DEFAULT NULL COMMENT '缇ょ粍ID',
  `request_id` bigint DEFAULT NULL COMMENT '鍊熼槄鐢宠ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍊熼槄璁板綍琛?;

-- ==========================================
-- 鏀惰棌琛細t_collect
-- ==========================================
DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鏀惰棌鐢ㄦ埛ID',
  `target_id` bigint NOT NULL COMMENT '鏀惰棌鐩爣ID',
  `collect_type` tinyint(1) NOT NULL COMMENT '鏀惰棌绫诲瀷锛?-鍥句功锛?-涔︽灦',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `collect_type`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_target_id` (`target_id`) USING BTREE,
  KEY `idx_collect_type` (`collect_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏀惰棌琛?;

-- ==========================================
-- 缇ょ粍琛細t_group
-- ==========================================
DROP TABLE IF EXISTS `t_group`;
CREATE TABLE `t_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `group_name` varchar(100) NOT NULL COMMENT '缇ょ粍鍚嶇О',
  `owner_id` bigint NOT NULL COMMENT '缇や富鐢ㄦ埛ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '缇ょ粍鎻忚堪',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_owner_id` (`owner_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缇ょ粍琛?;

-- ==========================================
-- 缇ょ粍鎴愬憳琛細t_group_user
-- ==========================================
DROP TABLE IF EXISTS `t_group_user`;
CREATE TABLE `t_group_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `group_id` bigint NOT NULL COMMENT '缇ょ粍ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍔犲叆鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缇ょ粍鎴愬憳琛?;


-- ==========================================
-- 缇ょ粍鍊熼槄鐢宠琛細t_group_borrow_request
-- ==========================================
DROP TABLE IF EXISTS `t_group_borrow_request`;
CREATE TABLE `t_group_borrow_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `group_id` bigint NOT NULL COMMENT '缇ょ粍ID',
  `book_id` bigint NOT NULL COMMENT '鍥句功ID',
  `book_name` varchar(255) NOT NULL COMMENT '鍥句功鍚嶇О蹇収',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '灏侀潰蹇収',
  `shelf_id` bigint DEFAULT NULL COMMENT '鏉ユ簮涔︽灦ID',
  `shelf_name` varchar(100) DEFAULT NULL COMMENT '鏉ユ簮涔︽灦鍚嶇О',
  `owner_user_id` bigint NOT NULL COMMENT '鍑哄€熶汉鐢ㄦ埛ID',
  `owner_nickname` varchar(50) DEFAULT NULL COMMENT '鍑哄€熶汉鏄电О蹇収',
  `borrower_user_id` bigint NOT NULL COMMENT '鍊熷叆浜虹敤鎴稩D',
  `borrower_nickname` varchar(50) DEFAULT NULL COMMENT '鍊熷叆浜烘樀绉板揩鐓?,
  `due_time` date DEFAULT NULL COMMENT '棰勮褰掕繕鏃ユ湡',
  `request_remark` varchar(500) DEFAULT NULL COMMENT '鐢宠澶囨敞',
  `borrow_flow_id` varchar(64) DEFAULT NULL COMMENT '鍊熼槄娴両D',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鐢宠鐘舵€侊細0-寰呭鐞嗭紝1-宸插悓鎰忥紝2-宸叉嫆缁?,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鍚︼紝1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_book_id` (`book_id`) USING BTREE,
  KEY `idx_owner_user_id` (`owner_user_id`) USING BTREE,
  KEY `idx_borrower_user_id` (`borrower_user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_borrow_flow_id` (`borrow_flow_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缇ょ粍鍊熼槄鐢宠琛?;
