-- ===========================================================================
-- 不挂高数社区 · 回滚脚本 003
-- 用途：撤销 003_migrate_all_questions_to_topic.sql 的话题迁移，把每个问题还原到迁移前的话题
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/003_rollback.sql
-- 依赖：questions / topics / question_topic_migration_log（由 003 迁移脚本建立）
--
-- 【说明】
--   * 只还原「本次迁移记录过的问题」，迁移后新建的问题不受影响；
--   * 依据日志表里的最早原值还原，因此先迁到 A 再迁到 B 也能一次回到最初状态；
--   * 原值为 NULL 的记录会被跳过（questions.topicId 已收紧为 NOT NULL，正常不会出现）；
--   * 还原后重算 topics.talkCount，与迁移脚本口径一致；
--   * 日志表默认保留以便追溯，确认无误后可手动删除（见文末）。
-- ===========================================================================

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

SET @batch_size = 5000;   -- 每批处理的 questionId 跨度

-- ---------------------------------------------------------------------------
-- 步骤 1：回滚前预览（只读）
-- ---------------------------------------------------------------------------
-- 1.1 将要还原到哪些话题、各多少条
SELECT COALESCE(t.title, '(无归属)') AS 目标话题, l.oldTopicId AS 原话题id, COUNT(*) AS 问题数
  FROM question_topic_migration_log l
  LEFT JOIN topics t ON t.id = l.oldTopicId
 GROUP BY l.oldTopicId, t.title
 ORDER BY 问题数 DESC;

-- 1.2 日志总量与其中无法还原的条数（原值为 NULL）
SELECT COUNT(*)                                                  AS 日志总条数,
       SUM(CASE WHEN oldTopicId IS NULL THEN 1 ELSE 0 END)       AS 无法还原条数
  FROM question_topic_migration_log;

-- ---------------------------------------------------------------------------
-- 步骤 2：回滚过程（按主键区间分批还原，避免长事务）
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `community_rollback_questions_topic`;
DELIMITER $$
CREATE PROCEDURE `community_rollback_questions_topic`(IN p_batch_size INT)
BEGIN
    DECLARE v_restored BIGINT DEFAULT 0;
    DECLARE v_skipped  BIGINT DEFAULT 0;
    DECLARE v_last_id  BIGINT DEFAULT 0;
    DECLARE v_max_id   BIGINT DEFAULT 0;

    IF p_batch_size IS NULL OR p_batch_size <= 0 THEN
        SET p_batch_size = 5000;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = DATABASE() AND table_name = 'question_topic_migration_log'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003 回滚: 找不到 question_topic_migration_log，无法还原（请确认 003 迁移脚本是否执行过）';
    END IF;

    SELECT COALESCE(MAX(questionId), 0) INTO v_max_id FROM questions;

    WHILE v_last_id < v_max_id DO
        UPDATE questions q
          JOIN question_topic_migration_log l ON l.questionId = q.questionId
           SET q.topicId = l.oldTopicId
         WHERE q.questionId > v_last_id
           AND q.questionId <= v_last_id + p_batch_size
           AND l.oldTopicId IS NOT NULL;
        SET v_restored = v_restored + ROW_COUNT();
        SET v_last_id  = v_last_id + p_batch_size;
    END WHILE;

    SELECT COUNT(*) INTO v_skipped
      FROM question_topic_migration_log
     WHERE oldTopicId IS NULL;

    -- 重算话题讨论数（与 TopicTagBackfillRunner / 迁移脚本口径一致）
    UPDATE topics t
       SET talkCount = (SELECT COUNT(*)
                          FROM questions q
                         WHERE q.topicId = t.id
                           AND q.status = 1
                           AND q.reviewStatus = 1);

    SELECT v_restored AS 已还原问题数, v_skipped AS 跳过的无原值条数;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 步骤 3：执行
-- ---------------------------------------------------------------------------
CALL community_rollback_questions_topic(@batch_size);

-- ---------------------------------------------------------------------------
-- 步骤 4：回滚后核对
-- ---------------------------------------------------------------------------
-- 4.1 话题分布是否恢复
SELECT t.id, t.title, t.talkCount, COUNT(q.questionId) AS 实际可见问题数
  FROM topics t
  LEFT JOIN questions q ON q.topicId = t.id AND q.status = 1 AND q.reviewStatus = 1
 GROUP BY t.id, t.title, t.talkCount
 ORDER BY 实际可见问题数 DESC, t.id;

-- 4.2 仍处于「迁移目标话题」且日志仍存在的残留（用于确认是否已全部还原）
SELECT COUNT(*) AS 可能残留条数
  FROM questions q
  JOIN question_topic_migration_log l ON l.questionId = q.questionId
 WHERE q.topicId = l.newTopicId
   AND l.oldTopicId IS NOT NULL
   AND NOT (q.topicId <=> l.oldTopicId);

-- ---------------------------------------------------------------------------
-- 可选清理（确认不需要再次回滚后手动执行）
-- ---------------------------------------------------------------------------
-- DROP TABLE IF EXISTS question_topic_migration_log;
-- DROP PROCEDURE IF EXISTS community_rollback_questions_topic;

SET SESSION sql_mode = @old_sql_mode;
