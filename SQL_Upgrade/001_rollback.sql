-- ===========================================================================
-- 001_performance_and_consistency.sql 的回滚脚本
-- 只移除新增的二级索引；不回滚字符集/字段类型（这些变更对应用无破坏性），
-- 不删除 classification 表（删除会丢失管理员配置的分类，请手动确认后再执行）
-- 可重复执行（幂等）
-- ===========================================================================

DROP PROCEDURE IF EXISTS `community_drop_index`;
DELIMITER $$
CREATE PROCEDURE `community_drop_index`(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
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

CALL community_drop_index('Questions',      'idx_questions_status_alter');
CALL community_drop_index('Questions',      'idx_questions_user_status');
CALL community_drop_index('Questions',      'idx_questions_class_status');
CALL community_drop_index('Questions',      'idx_questions_status_create');
CALL community_drop_index('Questions',      'idx_questions_tag');
CALL community_drop_index('comment',        'idx_comment_question_type_status');
CALL community_drop_index('comment',        'idx_comment_parent');
CALL community_drop_index('notification',   'idx_notification_receiver_status');
CALL community_drop_index('notification',   'idx_notification_receiver_type');
CALL community_drop_index('clickLike',      'idx_clicklike_unique_check');
CALL community_drop_index('clickLike',      'idx_clicklike_question');
CALL community_drop_index('followTopic',    'idx_followtopic_user_topic');
CALL community_drop_index('followQuestion', 'idx_followquestion_user_question');
CALL community_drop_index('followUser',     'idx_followuser_user_follow');
CALL community_drop_index('topic',          'idx_topic_type_status');
CALL community_drop_index('topic',          'idx_topic_title');
CALL community_drop_index('advertisement',  'idx_ad_status_end');
CALL community_drop_index('advertisement',  'idx_ad_position_status');
CALL community_drop_index('onlineUser',     'idx_online_expire');
CALL community_drop_index('admin',          'idx_admin_time');
CALL community_drop_index('classification', 'idx_classification_kind_status_sort');

DROP PROCEDURE IF EXISTS `community_drop_index`;

-- 如需彻底删除动态分类表（会丢失所有分类配置，请先备份）：
-- DROP TABLE IF EXISTS `classification`;
