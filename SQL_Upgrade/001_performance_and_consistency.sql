-- ===========================================================================
-- 不挂高数社区 · 数据库性能与一致性升级脚本 001
-- 适用：MySQL 8.0+ / MariaDB 10.4+，已在 2026-11-15 的 community 库上验证
-- 特性：可重复执行（幂等），不删除任何业务数据；建议先在测试库执行并备份
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/001_performance_and_consistency.sql
-- 回滚：SQL_Upgrade/001_rollback.sql（仅移除本脚本新增的索引）
-- ===========================================================================

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- 1. 统一字符集与排序规则，避免关联查询时因字符集不同导致索引失效
ALTER DATABASE community CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `users`           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `userPermission`  ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `onlineUser`      ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `Questions`       ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `comment`         ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `notification`    ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `admin`           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `clickLike`       ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `topic`           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `followTopic`     ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `followQuestion`  ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `followUser`      ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `advertisement`   ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 幂等建索引的辅助过程（MySQL 不支持 CREATE INDEX IF NOT EXISTS）
DROP PROCEDURE IF EXISTS `community_add_index`;
DELIMITER $$
CREATE PROCEDURE `community_add_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 3. 按实际查询模式补充复合索引
-- 首页: where status=1 order by alterTime / 按分类筛选 / 按标签 regexp
CALL community_add_index('Questions', 'idx_questions_status_alter',   '`status`, `alterTime`');
CALL community_add_index('Questions', 'idx_questions_user_status',     '`userId`, `status`');
CALL community_add_index('Questions', 'idx_questions_class_status',    '`classification`, `status`');
CALL community_add_index('Questions', 'idx_questions_status_create',   '`status`, `createTime`');
-- utf8mb4 下整列 256*4=1024 字节会超过旧版索引键上限 767，用 191 前缀（191*4=764）
-- tag varchar(256) > 191，因此前缀合法；搜索用 REGEXP 不会走该索引，但按标签精确匹配可以
CALL community_add_index('Questions', 'idx_questions_tag',             '`tag`(191)');

-- 评论: 按问题 + 类型取一级评论；按 parentId 取二级评论
CALL community_add_index('comment', 'idx_comment_question_type_status', '`questionId`, `type`, `status`');
CALL community_add_index('comment', 'idx_comment_parent',               '`parentId`, `status`');

-- 通知: 未读数 / 按类型分页
CALL community_add_index('notification', 'idx_notification_receiver_status', '`receiver`, `status`');
CALL community_add_index('notification', 'idx_notification_receiver_type',   '`receiver`, `type`, `status`');

-- 点赞: getClickLikePreventRepeat(notifier, questionId, commentId)
CALL community_add_index('clickLike', 'idx_clicklike_unique_check', '`notifier`, `questionId`, `commentId`');
CALL community_add_index('clickLike', 'idx_clicklike_question',     '`questionId`');

-- 关注关系: 是否已关注 / 关注列表 / 粉丝列表
CALL community_add_index('followTopic',    'idx_followtopic_user_topic',       '`userId`, `topicId`');
CALL community_add_index('followQuestion', 'idx_followquestion_user_question', '`userId`, `followQuestionId`');
CALL community_add_index('followUser',     'idx_followuser_user_follow',       '`userId`, `followUserId`');

-- 话题: 按分类展示 / 按标题查话题卡片 / 删除分类时按 type 统计
CALL community_add_index('topic', 'idx_topic_type_status', '`type`, `status`');
-- title varchar(100)，前缀长度不能超过列长度，因此直接对整列建索引
CALL community_add_index('topic', 'idx_topic_title',       '`title`');

-- 广告: 定时任务 where status=1；投放位置容量统计
CALL community_add_index('advertisement', 'idx_ad_status_end',      '`status`, `endTime`');
CALL community_add_index('advertisement', 'idx_ad_position_status', '`position`, `status`');

-- 在线用户过期清理 / 管理数据趋势图
CALL community_add_index('onlineUser', 'idx_online_expire', '`expireTime`');
CALL community_add_index('admin',      'idx_admin_time',    '`time`');

-- 分类表在下面第 4 节创建，创建完成后再补一次索引（老库已存在该表时也能补上）


-- 4. 动态分类表（与应用启动时 ClassificationSchemaInitializer 的建表语句一致）
CREATE TABLE IF NOT EXISTS `classification`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kind` varchar(20) NOT NULL COMMENT 'question=问题分类；topic=话题所属分类',
  `code` int NOT NULL COMMENT '稳定编号：问题分类用于 /?class=，话题分类写入 topic.type',
  `name` varchar(50) NOT NULL,
  `sortOrder` int DEFAULT 0,
  `status` int DEFAULT 1,
  `createTime` bigint DEFAULT NULL,
  `modifiedTime` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_classification_kind_code` (`kind`, `code`),
  UNIQUE KEY `uk_classification_kind_name` (`kind`, `name`),
  KEY `idx_classification_kind_status_sort` (`kind`, `status`, `sortOrder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CALL community_add_index('classification', 'idx_classification_kind_status_sort', '`kind`, `status`, `sortOrder`');
DROP PROCEDURE IF EXISTS `community_add_index`;

-- 5. 字段类型收敛（去掉 MySQL 8 已废弃的显示宽度、缩短过大的 URL 字段）
-- 这些 ALTER 可重复执行且不会丢数据；若存在超过 1000 字符的图片地址请跳过第 4、5 行
ALTER TABLE `topic`         MODIFY COLUMN `image`      varchar(1000) DEFAULT NULL,
                            MODIFY COLUMN `followCount` int DEFAULT 0,
                            MODIFY COLUMN `status`      tinyint DEFAULT 1;
ALTER TABLE `followTopic`   MODIFY COLUMN `topicImage` varchar(1000) DEFAULT NULL;
ALTER TABLE `advertisement` MODIFY COLUMN `status`     tinyint DEFAULT 0;
ALTER TABLE `Questions`     MODIFY COLUMN `tag`        varchar(256) DEFAULT NULL,
                            MODIFY COLUMN `status`     tinyint DEFAULT 1;

-- 6. 更新统计信息，让优化器立即使用新索引
ANALYZE TABLE `Questions`, `comment`, `notification`, `clickLike`,
              `topic`, `advertisement`, `followTopic`, `followQuestion`,
              `followUser`, `onlineUser`, `admin`, `classification`;

SET SESSION sql_mode = @old_sql_mode;

-- 验证：SHOW INDEX FROM Questions; 应能看到 idx_questions_* 系列索引
--       EXPLAIN SELECT * FROM Questions WHERE status=1 ORDER BY alterTime DESC LIMIT 15;
--       预期 key = idx_questions_status_alter，且不会出现 Using filesort 的全表扫描
