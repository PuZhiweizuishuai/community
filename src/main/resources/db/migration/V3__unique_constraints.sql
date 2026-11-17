-- ===========================================================================
-- 不挂高数社区 · 数据库一致性升级脚本 002
-- 内容：点赞/关注四张表清理重复数据并补唯一索引（C-1）；onlineUser.token 扩宽（Q-3）
-- 适用：MySQL 8.0+，需先执行 001_performance_and_consistency.sql
-- 特性：可重复执行（幂等）；清理重复数据时保留最早一条记录
-- 注意：历史重复点赞/关注可能已使计数字段漂移，唯一索引只能防止新增漂移；
--       如需校正存量计数，可在业务低峰期按关系表 COUNT 重算 Questions/topic/users 的计数字段
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/002_unique_constraints.sql
-- 回滚：SQL_Upgrade/002_rollback.sql（仅移除本脚本新增的唯一索引）
-- ===========================================================================

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- 1. 清理存量重复数据（保留主键最小的一条）
DELETE c1 FROM `clickLike` c1
JOIN `clickLike` c2
  ON c1.notifier = c2.notifier AND c1.questionId = c2.questionId AND c1.commentId = c2.commentId
 AND c1.likeId > c2.likeId;

DELETE f1 FROM `followUser` f1
JOIN `followUser` f2
  ON f1.userId = f2.userId AND f1.followUserId = f2.followUserId
 AND f1.id > f2.id;

DELETE f1 FROM `followQuestion` f1
JOIN `followQuestion` f2
  ON f1.userId = f2.userId AND f1.followQuestionId = f2.followQuestionId
 AND f1.id > f2.id;

DELETE f1 FROM `followTopic` f1
JOIN `followTopic` f2
  ON f1.userId = f2.userId AND f1.topicId = f2.topicId
 AND f1.followTopicId > f2.followTopicId;

-- 2. 幂等建唯一索引的辅助过程
DROP PROCEDURE IF EXISTS `community_add_unique_index`;
DELIMITER $$
CREATE PROCEDURE `community_add_unique_index`(
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
        SET @ddl = CONCAT('CREATE UNIQUE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 3. 唯一索引：把判重交给数据库，并发双击/重复请求不再产生脏数据
CALL community_add_unique_index('clickLike',      'uk_like',            '`notifier`, `questionId`, `commentId`');
CALL community_add_unique_index('followUser',     'uk_follow_user',     '`userId`, `followUserId`');
CALL community_add_unique_index('followQuestion', 'uk_follow_question', '`userId`, `followQuestionId`');
CALL community_add_unique_index('followTopic',    'uk_follow_topic',    '`userId`, `topicId`');

DROP PROCEDURE IF EXISTS `community_add_unique_index`;

-- 4. 移除被唯一索引覆盖的冗余普通索引（存在才删）
DROP PROCEDURE IF EXISTS `community_drop_index`;
DELIMITER $$
CREATE PROCEDURE `community_drop_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('DROP INDEX `', p_index, '` ON `', p_table, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL community_drop_index('clickLike',      'idx_clicklike_unique_check');
CALL community_drop_index('followTopic',    'idx_followtopic_user_topic');
CALL community_drop_index('followQuestion', 'idx_followquestion_user_question');
CALL community_drop_index('followUser',     'idx_followuser_user_follow');

DROP PROCEDURE IF EXISTS `community_drop_index`;

-- 5. onlineUser.token 扩宽：中文用户名签发的 JWT 经 base64 膨胀后可能超过 300 字符
ALTER TABLE `onlineUser` MODIFY COLUMN `token` varchar(1024) DEFAULT NULL;

-- 6. 更新统计信息
ANALYZE TABLE `clickLike`, `followUser`, `followQuestion`, `followTopic`, `onlineUser`;

SET SESSION sql_mode = @old_sql_mode;

-- 验证：SHOW INDEX FROM clickLike; 应能看到 Non_unique=0 的 uk_like