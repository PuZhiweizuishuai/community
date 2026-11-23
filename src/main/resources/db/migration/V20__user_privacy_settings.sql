-- ===========================================================================
-- V20: 用户主页隐私开关
--
-- 允许用户控制个人主页是否公开：
--   关注的问题 / 关注的话题 / 粉丝列表 / 关注的人
-- 默认全部公开，存量用户行为不变。
--
-- 使用 information_schema 判断后动态执行，兼容 DDL 已执行但 Flyway 未记录
-- 的中断场景（MySQL 的 ALTER TABLE 会隐式提交，不能依赖事务回滚）。
-- ===========================================================================

SET @addShowFollowQuestions = IF(
        EXISTS(SELECT 1
               FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'users'
                 AND column_name = 'showFollowQuestions'),
        'SELECT 1',
        'ALTER TABLE `users` ADD COLUMN `showFollowQuestions` tinyint NOT NULL DEFAULT 1 COMMENT ''公开关注的问题：1 公开，0 隐藏'''
    );
PREPARE stmt FROM @addShowFollowQuestions;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addShowFollowTopics = IF(
        EXISTS(SELECT 1
               FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'users'
                 AND column_name = 'showFollowTopics'),
        'SELECT 1',
        'ALTER TABLE `users` ADD COLUMN `showFollowTopics` tinyint NOT NULL DEFAULT 1 COMMENT ''公开关注的话题：1 公开，0 隐藏'''
    );
PREPARE stmt FROM @addShowFollowTopics;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addShowFans = IF(
        EXISTS(SELECT 1
               FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'users'
                 AND column_name = 'showFans'),
        'SELECT 1',
        'ALTER TABLE `users` ADD COLUMN `showFans` tinyint NOT NULL DEFAULT 1 COMMENT ''公开粉丝列表：1 公开，0 隐藏'''
    );
PREPARE stmt FROM @addShowFans;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @addShowFollowingUsers = IF(
        EXISTS(SELECT 1
               FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'users'
                 AND column_name = 'showFollowingUsers'),
        'SELECT 1',
        'ALTER TABLE `users` ADD COLUMN `showFollowingUsers` tinyint NOT NULL DEFAULT 1 COMMENT ''公开关注的人：1 公开，0 隐藏'''
    );
PREPARE stmt FROM @addShowFollowingUsers;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
