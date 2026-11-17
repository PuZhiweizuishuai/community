-- ===========================================================================
-- V8: 问题内容拆表 question_content
-- 一个问题最多 3 个内容段落：0 普通文本 / 1 密码可见 / 2 回复可见
-- 密码为明文存储（仅用于阅读校验，不做真实加密）
-- Questions.description 保留为公开普通文本副本，受保护段落不写入该列
-- ===========================================================================

CREATE TABLE IF NOT EXISTS question_content
(
    id         bigint PRIMARY KEY AUTO_INCREMENT,
    questionId bigint      NOT NULL,
    -- 段落顺序，0 起，最多 3 段
    sort       int         NOT NULL DEFAULT 0,
    -- 0 普通文本；1 密码可见；2 回复可见
    type       tinyint     NOT NULL DEFAULT 0,
    content    longtext    NOT NULL,
    -- 明文阅读密码，仅 type=1 使用
    password   varchar(64) DEFAULT NULL,
    KEY idx_question_sort (questionId, sort),
    CONSTRAINT fk_content_question FOREIGN KEY (questionId) REFERENCES Questions (questionId)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 存量问题正文迁移为单个普通段落（含已删除问题，恢复后正文不丢失）
INSERT INTO question_content (questionId, sort, type, content)
SELECT q.questionId, 0, 0, q.description
FROM Questions q
WHERE q.description IS NOT NULL
  AND q.description <> ''
  AND NOT EXISTS (SELECT 1 FROM question_content qc WHERE qc.questionId = q.questionId);

ANALYZE TABLE question_content;