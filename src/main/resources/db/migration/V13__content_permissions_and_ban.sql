-- ===========================================================================
-- V13: 社区内容权限 + 用户封禁
--
-- 1. 把发布问题、发表评论、上传文件纳入 RBAC 权限点，新增默认角色「社区成员」
--    并分配给所有存量用户；新用户注册时由代码分配该角色。
-- 2. 新增封禁用户权限与用户封禁字段、封禁流水表，支持临时封禁与永久封禁。
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 用户封禁状态（冗余到 users，鉴权时无需每次查日志表）
-- ---------------------------------------------------------------------------
ALTER TABLE `users`
    ADD COLUMN `banType`     tinyint      NOT NULL DEFAULT 0 COMMENT '0 正常，1 临时封禁，2 永久封禁',
    ADD COLUMN `banEndTime`  bigint       NOT NULL DEFAULT 0 COMMENT '临时封禁结束时间（毫秒），永久封禁为 0',
    ADD COLUMN `banReason`   varchar(255) DEFAULT NULL COMMENT '封禁原因',
    ADD COLUMN `banTime`     bigint       NOT NULL DEFAULT 0 COMMENT '最近一次封禁时间',
    ADD COLUMN `banOperator` bigint       NOT NULL DEFAULT 0 COMMENT '最近一次封禁操作人';

CREATE INDEX `idx_users_ban` ON `users` (`banType`, `banEndTime`);

CREATE TABLE IF NOT EXISTS `user_ban_log`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `userId`          bigint       NOT NULL,
    `banType`         tinyint      NOT NULL COMMENT '1 临时封禁，2 永久封禁',
    `endTime`         bigint       NOT NULL DEFAULT 0 COMMENT '临时封禁结束时间，永久封禁为 0',
    `reason`          varchar(255) DEFAULT NULL,
    `operatorId`      bigint       NOT NULL COMMENT '封禁操作人',
    `createTime`      bigint       NOT NULL,
    `status`          tinyint      NOT NULL DEFAULT 1 COMMENT '1 生效中，0 已解除',
    `liftedTime`      bigint       DEFAULT NULL COMMENT '解封时间',
    `liftOperatorId`  bigint       DEFAULT NULL COMMENT '解封操作人',
    PRIMARY KEY (`id`),
    KEY `idx_user_ban_log_user` (`userId`, `status`, `id`),
    CONSTRAINT `fk_user_ban_log_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 新权限点
-- ---------------------------------------------------------------------------
INSERT INTO `sys_permission` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('admin:user:ban', '封禁用户', '用户', '临时或永久封禁用户，并支持解封', 22),
       ('content:question:publish', '发布问题', '社区', '发布与编辑社区问题', 60),
       ('content:comment:create', '发表评论', '社区', '对问题发表评论与回复', 61),
       ('file:upload', '上传文件', '社区', '在编辑器与附件中上传文件', 62)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `groupName`   = VALUES(`groupName`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`);

-- ---------------------------------------------------------------------------
-- 默认角色「社区成员」：所有注册用户的基础互动权限
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role` (`code`, `name`, `description`, `builtIn`, `status`, `sortOrder`, `createTime`, `updateTime`)
VALUES ('user', '社区成员', '默认角色：发布问题、发表评论与上传文件', 1, 1, 5,
        UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`),
                        `updateTime`  = VALUES(`updateTime`);

-- 超级管理员始终拥有全部权限（PermissionChecker 同时按角色编码兜底）
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         CROSS JOIN `sys_permission` p
WHERE r.`code` = 'super_admin';

-- 社区成员：基础互动权限
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN ('content:question:publish', 'content:comment:create', 'file:upload')
WHERE r.`code` = 'user';

-- 运营管理员负责用户运营，默认授予封禁权限
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` = 'admin:user:ban'
WHERE r.`code` = 'ops_admin';

-- 所有存量用户（含管理员）默认拥有「社区成员」角色
INSERT IGNORE INTO `sys_user_role` (`userId`, `roleId`)
SELECT u.`id`, r.`id`
FROM `users` u
         CROSS JOIN `sys_role` r
WHERE r.`code` = 'user';
