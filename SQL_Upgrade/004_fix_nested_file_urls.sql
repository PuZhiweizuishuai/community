-- ===========================================================================
-- 不挂高数社区 · 数据修复脚本 004
-- 用途：修复富文本里「文件链接被重复拼接」的坏链
--
--   坏：/api/file/0/admin/api/file/0/79a16ae0-e59c-43a5-8bfe-9953d885620f.docx
--   好：/api/file/0/admin/file/79a16ae0-e59c-43a5-8bfe-9953d885620f.docx
--
--   规则：保留最外层 /api/file/{存储id}/{用户名}/ 前缀，删掉内层多余的 api/file/{存储id}/，
--        只留下 file/ + 文件名。修完后的地址与 file_records.url 完全一致。
--
-- 影响表：question_contents.content、comments.content、questions.description
--        （notifications.commentContent 只是评论内容快照，执行前统计为 0 条重复链接，故不处理）
--
-- 适用：MySQL 8.0.4+（依赖 REGEXP_REPLACE 的 $1 反向引用与 ICU 正则）
--       MariaDB 的 REGEXP_REPLACE 语法不兼容，勿直接在 MariaDB 上执行
-- 执行：mysql -h<host> -u<user> -p community < SQL_Upgrade/004_fix_nested_file_urls.sql
-- 回滚：SQL_Upgrade/004_rollback.sql（依赖本脚本建立的 file_url_fix_log 表）
--
-- 【执行前必做】
--   备份：mysqldump -h<host> -u<user> -p community question_contents comments questions \
--         > backup_$(date +%F).sql
--
-- 【幂等性】重复执行安全：只更新仍命中「重复链接」正则的行；
--   被修改过的行会把原值写入 file_url_fix_log（tbl + rowId 唯一，只保留最早一次的原值），
--   所以同一条数据反复修、多次修也只会留下最初备份，回滚能一次回到最初状态。
--
-- 【实现备注 1】模式串与替换串在下面各处直接内联，没有用 @变量。
--   原因：用户变量是 IMPLICIT 排序规则，和列上的 utf8mb4_unicode_ci 会撞出
--   "Illegal mix of collations ... for operation 'regexp_like'"；内联字面量可被自动转换。
--
-- 【实现备注 2】预览与校验都是「每张表一条 SELECT」，不写成 UNION ALL。
--   原因：question_contents.content 建表时用了 utf8mb4_0900_ai_ci，
--   而 comments.content / questions.description 是 utf8mb4_unicode_ci，
--   三者放进同一个 UNION 列会报 "Illegal mix of collations for operation 'UNION'"。
--
-- 【已知边界】本脚本统一把缺省的类型目录补成 file/，因为这类重复链接都是下载类文件
--   （pdf / pptx / docx / zip / mp3 / mp4），实际落盘位置都是 {用户名}/file/。
--   如果哪天出现被重复拼接的「图片」链接，它应该补成 image/，跑完用「步骤 4.3」对照能发现。
-- ===========================================================================

-- 修复用的正则与替换（脚本里各处内联的是同一份）：
--   pattern     : /api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/
--   replacement : /api/file/$1/$2/file/

SET @old_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- ---------------------------------------------------------------------------
-- 步骤 1：执行前预览（只读，先看清会动哪些数据）
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS question_contents_待修行数
  FROM question_contents
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS comments_待修行数
  FROM comments
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS questions_待修行数
  FROM questions
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

-- 1.1 抽样看被命中的链接长什么样
SELECT id AS 行id,
       REGEXP_SUBSTR(content, '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/', 1, 1) AS 重复链接样例
  FROM question_contents
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/'
 LIMIT 10;

SELECT commentId AS 行id,
       REGEXP_SUBSTR(content, '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/', 1, 1) AS 重复链接样例
  FROM comments
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/'
 LIMIT 10;

SELECT questionId AS 行id,
       REGEXP_SUBSTR(description, '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/', 1, 1) AS 重复链接样例
  FROM questions
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/'
 LIMIT 10;

-- ---------------------------------------------------------------------------
-- 步骤 2：原值留底表（回滚依据；已存在则跳过）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_url_fix_log (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    tbl       VARCHAR(64)  NOT NULL COMMENT '被修改的表名',
    rowId     BIGINT       NOT NULL COMMENT '被修改行的主键值',
    oldValue  MEDIUMTEXT   NULL     COMMENT '修复前的内容（回滚用）',
    fixedAt   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修复时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tbl_row (tbl, rowId)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '004 文件链接修复留底表';

-- ---------------------------------------------------------------------------
-- 步骤 3：修复（三张表同一规则）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO file_url_fix_log (tbl, rowId, oldValue)
SELECT 'question_contents', id, content
  FROM question_contents
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

INSERT IGNORE INTO file_url_fix_log (tbl, rowId, oldValue)
SELECT 'comments', commentId, content
  FROM comments
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

INSERT IGNORE INTO file_url_fix_log (tbl, rowId, oldValue)
SELECT 'questions', questionId, description
  FROM questions
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

UPDATE question_contents
   SET content = REGEXP_REPLACE(content,
                                '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/',
                                '/api/file/$1/$2/file/')
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

UPDATE comments
   SET content = REGEXP_REPLACE(content,
                                '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/',
                                '/api/file/$1/$2/file/')
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

UPDATE questions
   SET description = REGEXP_REPLACE(description,
                                    '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/',
                                    '/api/file/$1/$2/file/')
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

-- ---------------------------------------------------------------------------
-- 步骤 4：执行后校验
-- ---------------------------------------------------------------------------
-- 4.1 残留的重复链接必须为 0
SELECT COUNT(*) AS question_contents_残留重复链接
  FROM question_contents
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS comments_残留重复链接
  FROM comments
 WHERE content REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

SELECT COUNT(*) AS questions_残留重复链接
  FROM questions
 WHERE description REGEXP '/api/file/([0-9]+)/([^/"<>[:space:]]+)/api/file/[0-9]+/';

-- 4.2 修复明细（人工抽查用）
SELECT tbl AS 表, rowId AS 行id, CHAR_LENGTH(oldValue) AS 修复前长度, fixedAt AS 修复时间
  FROM file_url_fix_log
 ORDER BY id DESC
 LIMIT 50;

-- 4.3 权威对照（可选）：把 <文件名> 换成 4.2 里抽查到的文件名，
--     url 列就是系统认定的正确地址，应与内容里的链接完全一致；
--     若 url 指向 image/ 而内容里是 file/，按 url 修正这一条。
-- SELECT storageId, fileKey, url FROM file_records WHERE fileKey LIKE '%<文件名>' LIMIT 5;

SET SESSION sql_mode = @old_sql_mode;
