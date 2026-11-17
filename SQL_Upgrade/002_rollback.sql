-- ===========================================================================
-- 002_unique_constraints.sql 的回滚脚本：仅移除新增唯一索引，恢复被替换的普通索引
-- 注意：已被清理的重复数据无法恢复
-- ===========================================================================

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

CALL community_drop_index('clickLike',      'uk_like');
CALL community_drop_index('followUser',     'uk_follow_user');
CALL community_drop_index('followQuestion', 'uk_follow_question');
CALL community_drop_index('followTopic',    'uk_follow_topic');

DROP PROCEDURE IF EXISTS `community_drop_index`;

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

CALL community_add_index('clickLike',      'idx_clicklike_unique_check',       '`notifier`, `questionId`, `commentId`');
CALL community_add_index('followTopic',    'idx_followtopic_user_topic',       '`userId`, `topicId`');
CALL community_add_index('followQuestion', 'idx_followquestion_user_question', '`userId`, `followQuestionId`');
CALL community_add_index('followUser',     'idx_followuser_user_follow',       '`userId`, `followUserId`');

DROP PROCEDURE IF EXISTS `community_add_index`;

ALTER TABLE `onlineUser` MODIFY COLUMN `token` varchar(300) DEFAULT NULL;