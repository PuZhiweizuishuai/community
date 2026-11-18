-- ===========================================================================
-- V14: 评论管理
--
-- 1. 新增 admin:comment 权限点，默认授予超级管理员与内容审核员；
-- 2. 补充评论状态索引，后台按「正常 / 已删除」筛选并按评论 ID 倒序分页。
-- ===========================================================================

INSERT INTO `sys_permission` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('admin:comment', '评论管理', '内容', '查看、删除与恢复评论及其上下文', 33)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `groupName`   = VALUES(`groupName`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`);

-- 超级管理员与内容审核员默认拥有评论管理权限
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` = 'admin:comment'
WHERE r.`code` IN ('super_admin', 'content_admin');

CREATE INDEX `idx_comment_status_id` ON `comment` (`status`, `commentId`);
