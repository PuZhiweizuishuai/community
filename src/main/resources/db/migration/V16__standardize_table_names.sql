-- ===========================================================================
-- V16: 表名统一为小写复数 snake_case
--
-- 历史迁移继续使用旧表名，本迁移在最新结构上一次性重命名；
-- MySQL 的 RENAME TABLE 会同步更新外键引用，flyway_schema_history 保持不动。
-- 用户表 users 已符合小写复数规范，无需改名。
-- ===========================================================================

RENAME TABLE
    `Questions`           TO `questions`,
    `admin`               TO `admin_snapshots`,
    `advertisement`       TO `advertisements`,
    `classification`      TO `classifications`,
    `clickLike`           TO `click_likes`,
    `comment`             TO `comments`,
    `content_report`      TO `content_reports`,
    `content_review_log`  TO `content_review_logs`,
    `file_record`         TO `file_records`,
    `followQuestion`      TO `follow_questions`,
    `followTopic`         TO `follow_topics`,
    `followUser`          TO `follow_users`,
    `notification`        TO `notifications`,
    `onlineUser`          TO `online_users`,
    `question_content`    TO `question_contents`,
    `storage_config`      TO `storage_configs`,
    `sys_permission`      TO `sys_permissions`,
    `sys_role`            TO `sys_roles`,
    `sys_role_permission` TO `sys_role_permissions`,
    `sys_user_role`       TO `sys_user_roles`,
    `system_config`       TO `system_configs`,
    `topic`               TO `topics`,
    `userPermission`      TO `user_permissions`,
    `user_ban_log`        TO `user_ban_logs`;
