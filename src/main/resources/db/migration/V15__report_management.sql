-- ===========================================================================
-- V15: 举报管理与站内搜索下线
--
-- 1. 新增用户举报表，支持对问题 / 评论的举报、受理与驳回；
-- 2. 新增 admin:report（后台举报管理）与 content:report:create（用户举报）权限；
-- 3. 移除已下线的后台站内搜索权限点。
-- ===========================================================================

CREATE TABLE IF NOT EXISTS `content_report`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `targetType`      varchar(16)  NOT NULL COMMENT 'question / comment',
    `targetId`        bigint       NOT NULL COMMENT '被举报问题 / 评论 ID',
    `questionId`      bigint       NOT NULL DEFAULT 0 COMMENT '评论举报时所属问题 ID',
    `targetUserId`    bigint       NOT NULL DEFAULT 0 COMMENT '被举报内容作者',
    `targetSnapshot`  varchar(1000) DEFAULT NULL COMMENT '举报时的内容快照',
    `reporterId`      bigint       NOT NULL COMMENT '举报人',
    `reason`          varchar(32)  NOT NULL COMMENT 'spam / abuse / porn / illegal / other',
    `description`     varchar(500) DEFAULT NULL COMMENT '补充说明',
    `status`          tinyint      NOT NULL DEFAULT 0 COMMENT '0 待处理，1 已受理，2 已驳回',
    `handlerId`       bigint       NOT NULL DEFAULT 0 COMMENT '处理人',
    `handleNote`      varchar(500) DEFAULT NULL COMMENT '处理说明',
    `handleTime`      bigint       NOT NULL DEFAULT 0 COMMENT '处理时间',
    `contentRemoved`  tinyint      NOT NULL DEFAULT 0 COMMENT '受理时是否同步删除内容',
    `createTime`      bigint       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_content_report_status_id` (`status`, `id`),
    KEY `idx_content_report_target` (`targetType`, `targetId`),
    KEY `idx_content_report_reporter` (`reporterId`, `createTime`),
    KEY `idx_content_report_duplicate` (`targetType`, `targetId`, `reporterId`, `status`),
    KEY `idx_content_report_question` (`questionId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `sys_permission` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('admin:report', '举报管理', '内容', '查看、受理与驳回用户举报', 34),
       ('content:report:create', '举报内容', '社区', '举报问题与评论', 63)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `groupName`   = VALUES(`groupName`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`);

-- 超级管理员与内容审核员默认拥有后台举报管理权限
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` = 'admin:report'
WHERE r.`code` IN ('super_admin', 'content_admin');

-- 社区成员默认可举报内容
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` = 'content:report:create'
WHERE r.`code` = 'user';

-- 站内搜索模块已下线：删除权限点，角色关联由外键级联清理
DELETE FROM `sys_permission` WHERE `code` = 'admin:search';
