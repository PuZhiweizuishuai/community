-- ===========================================================================
-- V18: 用户时间字段统一为毫秒时间戳（BIGINT）
--
-- users.creationTime / users.lastTime 与 user_permissions.updateTime 历史定义为
-- VARCHAR(19)（yyyy-MM-dd HH:mm:ss），无法高效排序与范围查询；本迁移把它们
-- 统一为与其余业务表一致的 BIGINT（毫秒）。
--
-- 兼容处理：
--   * 已是数字的值原样保留（重复执行或手工改过类型时不会二次转换）；
--   * 空字符串 / NULL 转 NULL（user_permissions.updateTime 非空，转 0）；
--   * 无法解析的脏数据转 NULL / 0，避免 ALTER 阶段隐式转换报错。
-- ===========================================================================

UPDATE `users`
SET `creationTime` = CASE
                         WHEN `creationTime` IS NULL OR CHAR_LENGTH(TRIM(`creationTime`)) = 0 THEN NULL
                         WHEN `creationTime` REGEXP '^[0-9]+$' THEN `creationTime`
                         ELSE UNIX_TIMESTAMP(STR_TO_DATE(`creationTime`, '%Y-%m-%d %H:%i:%s')) * 1000
    END
WHERE `creationTime` IS NOT NULL;

UPDATE `users`
SET `lastTime` = CASE
                     WHEN `lastTime` IS NULL OR CHAR_LENGTH(TRIM(`lastTime`)) = 0 THEN NULL
                     WHEN `lastTime` REGEXP '^[0-9]+$' THEN `lastTime`
                     ELSE UNIX_TIMESTAMP(STR_TO_DATE(`lastTime`, '%Y-%m-%d %H:%i:%s')) * 1000
    END
WHERE `lastTime` IS NOT NULL;

ALTER TABLE `users`
    MODIFY COLUMN `creationTime` BIGINT NULL COMMENT '注册时间（毫秒时间戳）',
    MODIFY COLUMN `lastTime` BIGINT NULL COMMENT '上次登录时间（毫秒时间戳）';

UPDATE `user_permissions`
SET `updateTime` = CASE
                       WHEN `updateTime` IS NULL OR CHAR_LENGTH(TRIM(`updateTime`)) = 0 THEN 0
                       WHEN `updateTime` REGEXP '^[0-9]+$' THEN `updateTime`
                       ELSE COALESCE(UNIX_TIMESTAMP(STR_TO_DATE(`updateTime`, '%Y-%m-%d %H:%i:%s')) * 1000, 0)
    END;

ALTER TABLE `user_permissions`
    MODIFY COLUMN `updateTime` BIGINT NOT NULL DEFAULT 0 COMMENT '修改时间（毫秒时间戳）';
