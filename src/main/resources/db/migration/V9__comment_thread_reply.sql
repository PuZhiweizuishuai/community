-- ===========================================================================
-- V9: 评论二级回复扁平化
-- 一条一级评论下的所有回复统一归属根评论：parentCommentId = 根评论 ID，
-- parentId 保留被回复的具体评论，用于展示「回复 @xxx」。
-- 这样回复一级评论、回复二级评论都会出现在同一条评论的回复列表中。
-- ===========================================================================

-- 1. 修复历史数据：早期「回复二级评论」把 parentCommentId 写成了被回复评论的 ID
UPDATE comment c
JOIN comment p ON c.parentCommentId = p.commentId
SET c.parentCommentId = p.parentCommentId
WHERE c.type = 2
  AND p.type = 2
  AND p.parentCommentId > 0
  AND c.parentId = p.commentId;

-- 2. 根评论回复数按扁平化后的真实回复数重算
UPDATE comment r
LEFT JOIN (
    SELECT parentCommentId, COUNT(*) AS replyCount
    FROM comment
    WHERE type = 2 AND status = 1
    GROUP BY parentCommentId
) t ON t.parentCommentId = r.commentId
SET r.commentCount = COALESCE(t.replyCount, 0)
WHERE r.type = 1;

-- 3. 二级评论不再承载回复数
UPDATE comment SET commentCount = 0 WHERE type = 2;

-- 4. 按根评论查询全部回复的复合索引（幂等：已存在时执行 DO 0）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'comment'
                     AND index_name = 'idx_comment_parent_comment');
SET @ddl = IF(@idx_exists = 0,
    'CREATE INDEX idx_comment_parent_comment ON comment (parentCommentId, type, status)',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
