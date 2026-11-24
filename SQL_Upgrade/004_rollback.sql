-- ===========================================================================
-- 不挂高数社区 · 回滚脚本 004
-- 用途：撤销 004_fix_nested_file_urls.sql 的文件链接修复，把内容还原到修复前的原值
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/004_rollback.sql
-- 依赖：file_url_fix_log（由 004 修复脚本建立并写入原值）
--
-- 【说明】
--   * 只还原留底表里记录过的行，修复之后新产生的数据不受影响；
--   * 每行只保留「最早一次修复前」的原值（uk_tbl_row 唯一约束），
--     因此同一行被修过多次也只还原到最初状态，不会叠加出更乱的链接；
--   * 留底表默认保留以便追溯，确认无误后可手动删除（见文末）。
-- ===========================================================================

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- ---------------------------------------------------------------------------
-- 步骤 1：回滚前预览（只读）
-- ---------------------------------------------------------------------------
SELECT tbl AS 表, COUNT(*) AS 待还原行数, MIN(fixedAt) AS 最早修复时间, MAX(fixedAt) AS 最近修复时间
  FROM file_url_fix_log
 GROUP BY tbl;

-- ---------------------------------------------------------------------------
-- 步骤 2：还原
-- ---------------------------------------------------------------------------
UPDATE question_contents q
  JOIN file_url_fix_log l ON l.tbl = 'question_contents' AND l.rowId = q.id
   SET q.content = l.oldValue;

UPDATE comments c
  JOIN file_url_fix_log l ON l.tbl = 'comments' AND l.rowId = c.commentId
   SET c.content = l.oldValue;

UPDATE questions q
  JOIN file_url_fix_log l ON l.tbl = 'questions' AND l.rowId = q.questionId
   SET q.description = l.oldValue;

-- ---------------------------------------------------------------------------
-- 步骤 3：回滚后校验：重复链接行数应回到修复前的数量（明显 > 0 说明还原成功）；
--   校验同样分表查、不用 UNION（question_contents.content 与另外两张表的排序规则不同）
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS question_contents_重复链接行数
  FROM question_contents
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS comments_重复链接行数
  FROM comments
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS questions_重复链接行数
  FROM questions
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

-- ---------------------------------------------------------------------------
-- 可选：确认回滚结果无误后，清理留底表
-- ---------------------------------------------------------------------------
-- DROP TABLE IF EXISTS file_url_fix_log;

SET SESSION sql_mode = @old_sql_mode;
