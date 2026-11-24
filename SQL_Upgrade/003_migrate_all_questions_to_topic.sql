-- ===========================================================================
-- 不挂高数社区 · 数据迁移脚本 003
-- 用途：把现有全部问题（questions.topicId）统一归到指定的一个话题下
-- 适用：MySQL 8.0+ / MariaDB 10.4+（表结构基于 Flyway V20，questions.topicId 为 NOT NULL）
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/003_migrate_all_questions_to_topic.sql
-- 回滚：SQL_Upgrade/003_rollback.sql
--
-- 【执行前必做】
--   1) 备份：mysqldump -h<host> -u<user> -p community questions topics > backup_$(date +%F).sql
--   2) 改下面「步骤 1」里的 @target_topic_id，并用「步骤 2」的查询核对目标话题
--
-- 【安全约束（不满足会主动中止，不会写入任何数据）】
--   * 目标话题必须存在、status=1（未删除）、reviewStatus=1（已通过审核）
--   * 目标话题必须 visibility=0（公开）。若确需迁入私有话题，注释掉对应校验段
--
-- 【副作用提醒】
--   * 问题可见性跟着话题走（QuestionAccessService 先判话题）：迁入私有话题后，
--     非话题成员连标题都看不到，普通用户视角等同「全站清空」；
--   * topics.talkCount 是冗余统计列，脚本结尾会全量重算，原话题会归零、目标话题会集中全部问题；
--   * 问题的话题内置顶 isTop 会跟着搬过去，如需清空见文末「可选清理」；
--   * 不改 createTime / alterTime / reviewStatus / reviewReason，列表排序与审核状态保持不变；
--   * 不涉及 tags / question_tags，标签是全局的，不受话题迁移影响。
--
-- 【幂等性】重复执行安全：已在该话题下的问题会被跳过；
--   原话题记录写入 question_topic_migration_log（主键 questionId，只保留最早一次的原值）。
-- ===========================================================================

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- ---------------------------------------------------------------------------
-- 步骤 1：参数（按需修改）
-- ---------------------------------------------------------------------------
SET @target_topic_id = 1;      -- ← 改成目标话题 id
SET @batch_size      = 5000;   -- 每批处理的 questionId 跨度，大表可调小

-- ---------------------------------------------------------------------------
-- 步骤 2：执行前预览（只读，先看清会动哪些数据）
-- ---------------------------------------------------------------------------
-- 2.1 可用作目标的话题清单（挑一个 id 填到步骤 1）
SELECT id, title, talkCount, visibility, status, reviewStatus
  FROM topics
 WHERE status = 1 AND reviewStatus = 1
 ORDER BY id;

-- 2.2 目标话题信息
SELECT id, title, visibility, talkCount,
       CASE WHEN visibility = 0 THEN '公开，迁移后所有人可见'
            ELSE '私有，迁移后非成员将看不到任何问题' END AS 迁移影响
  FROM topics
 WHERE id = @target_topic_id;

-- 2.3 待迁移问题总数 + 当前分布（前 20 个话题）
SELECT COUNT(*) AS 待迁移问题数 FROM questions WHERE NOT (topicId <=> @target_topic_id);

SELECT COALESCE(t.title, '(无归属)') AS 原话题, COUNT(*) AS 问题数
  FROM questions q
  LEFT JOIN topics t ON t.id = q.topicId
 WHERE NOT (q.topicId <=> @target_topic_id)
 GROUP BY q.topicId, t.title
 ORDER BY 问题数 DESC
 LIMIT 20;

-- ---------------------------------------------------------------------------
-- 步骤 3：迁移日志表（回滚依据，首次执行自动建）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_topic_migration_log`
(
    `questionId`  bigint   NOT NULL COMMENT '问题 id',
    `oldTopicId`  bigint   DEFAULT NULL COMMENT '迁移前的话题 id（只保留最早一次的原值）',
    `newTopicId`  bigint   NOT NULL COMMENT '首次迁移的目标话题 id',
    `migrateTime` bigint   NOT NULL COMMENT '迁移时间（毫秒）',
    PRIMARY KEY (`questionId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '003 脚本：问题话题迁移前的原值备份';

-- ---------------------------------------------------------------------------
-- 步骤 4：迁移过程（校验 → 备份原值 → 主键区间分批 update → 重算 talkCount）
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `community_migrate_all_questions_to_topic`;
DELIMITER $$
CREATE PROCEDURE `community_migrate_all_questions_to_topic`(
    IN p_target_topic_id BIGINT,
    IN p_batch_size INT
)
BEGIN
    DECLARE v_topic_count   INT     DEFAULT 0;
    DECLARE v_status        INT     DEFAULT NULL;
    DECLARE v_review_status TINYINT DEFAULT NULL;
    DECLARE v_visibility    TINYINT DEFAULT NULL;
    DECLARE v_pending       BIGINT  DEFAULT 0;
    DECLARE v_moved         BIGINT  DEFAULT 0;
    DECLARE v_last_id       BIGINT  DEFAULT 0;
    DECLARE v_max_id        BIGINT  DEFAULT 0;
    DECLARE v_now           BIGINT  DEFAULT 0;

    -- 4.1 参数与目标话题校验
    IF p_target_topic_id IS NULL OR p_target_topic_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003: 目标话题 id 无效，请先设置 @target_topic_id';
    END IF;

    SELECT COUNT(*), MAX(status), MAX(reviewStatus), MAX(visibility)
      INTO v_topic_count, v_status, v_review_status, v_visibility
      FROM topics
     WHERE id = p_target_topic_id;

    IF v_topic_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003: 目标话题不存在，请检查 @target_topic_id';
    END IF;
    IF v_status IS NULL OR v_status <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003: 目标话题已被删除或禁用（topics.status <> 1），已中止';
    END IF;
    IF v_review_status IS NULL OR v_review_status <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003: 目标话题未通过审核（topics.reviewStatus <> 1），已中止';
    END IF;

    -- 私有话题：迁入后普通用户将完全看不到这些内容。确需如此请注释掉本段。
    IF v_visibility IS NULL OR v_visibility <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '003: 目标话题不是公开话题，迁入后非成员将看不到所有问题，已中止（确需迁入请注释本段校验）';
    END IF;

    IF p_batch_size IS NULL OR p_batch_size <= 0 THEN
        SET p_batch_size = 5000;
    END IF;

    SET v_now = UNIX_TIMESTAMP() * 1000;

    -- 4.2 待迁移数量
    SELECT COUNT(*) INTO v_pending
      FROM questions
     WHERE NOT (topicId <=> p_target_topic_id);

    IF v_pending = 0 THEN
        SELECT '003: 所有问题已在目标话题下，无需迁移' AS result;
    ELSE
        -- 4.3 备份原话题（主键 questionId，重复执行不会覆盖最早的原值）
        INSERT IGNORE INTO question_topic_migration_log (questionId, oldTopicId, newTopicId, migrateTime)
        SELECT questionId, topicId, p_target_topic_id, v_now
          FROM questions
         WHERE NOT (topicId <=> p_target_topic_id);

        -- 4.4 按主键区间分批更新，避免一次性长事务锁表
        SELECT COALESCE(MAX(questionId), 0) INTO v_max_id FROM questions;
        SET v_last_id = 0;
        WHILE v_last_id < v_max_id DO
            UPDATE questions
               SET topicId = p_target_topic_id
             WHERE questionId > v_last_id
               AND questionId <= v_last_id + p_batch_size
               AND NOT (topicId <=> p_target_topic_id);
            SET v_moved   = v_moved + ROW_COUNT();
            SET v_last_id = v_last_id + p_batch_size;
        END WHILE;

        -- 4.5 重算话题讨论数（与 TopicTagBackfillRunner 口径一致：仅 status=1 且 reviewStatus=1）
        UPDATE topics t
           SET talkCount = (SELECT COUNT(*)
                              FROM questions q
                             WHERE q.topicId = t.id
                               AND q.status = 1
                               AND q.reviewStatus = 1);

        SELECT v_moved           AS 本次迁移问题数,
               v_pending         AS 迁移前待迁移数,
               p_target_topic_id AS 目标话题,
               (SELECT title FROM topics WHERE id = p_target_topic_id) AS 目标话题名,
               (SELECT talkCount FROM topics WHERE id = p_target_topic_id) AS 目标话题讨论数;
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 步骤 5：执行
-- ---------------------------------------------------------------------------
CALL community_migrate_all_questions_to_topic(@target_topic_id, @batch_size);

-- ---------------------------------------------------------------------------
-- 步骤 6：执行后核对
-- ---------------------------------------------------------------------------
-- 6.1 仍不在目标话题下的问题（正常应为 0）
SELECT COUNT(*) AS 未迁移问题数 FROM questions WHERE NOT (topicId <=> @target_topic_id);

-- 6.2 话题分布（应只剩目标话题有数据）
SELECT t.id, t.title, t.talkCount, COUNT(q.questionId) AS 实际可见问题数
  FROM topics t
  LEFT JOIN questions q ON q.topicId = t.id AND q.status = 1 AND q.reviewStatus = 1
 GROUP BY t.id, t.title, t.talkCount
 ORDER BY 实际可见问题数 DESC, t.id;

-- 6.3 迁移日志规模
SELECT COUNT(*) AS 日志条数, MIN(migrateTime) AS 首次迁移时间, MAX(migrateTime) AS 最近迁移时间
  FROM question_topic_migration_log;

-- ---------------------------------------------------------------------------
-- 可选清理（按需手动执行，默认不跑）
-- ---------------------------------------------------------------------------
-- 迁入后原话题的置顶帖会在目标话题里一起置顶，如需全部取消置顶：
-- UPDATE questions SET isTop = 0 WHERE NOT (topicId <=> @target_topic_id);

-- 迁移确认无误、不再需要回滚时，可删除过程（日志表建议保留一段时间）：
-- DROP PROCEDURE IF EXISTS community_migrate_all_questions_to_topic;

SET SESSION sql_mode = @old_sql_mode;
