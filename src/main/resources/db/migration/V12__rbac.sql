-- ===========================================================================
-- V12: 统一权限管理（RBAC）
--
-- 原 userPermission.power == 0 判断管理员的模式改为标准 RBAC：
--   sys_role            角色
--   sys_permission      权限点（系统预置，后台按角色动态勾选）
--   sys_role_permission 角色 - 权限
--   sys_user_role       用户 - 角色
--
-- 存量 power = 0 的账号迁移到内置「超级管理员」角色，随后 power 统一回退为
-- 普通用户等级（1），power 只保留 VIP 等用户等级语义，不再参与管理员鉴权。
-- ===========================================================================

CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `code`        varchar(64)  NOT NULL COMMENT '角色编码，创建后不可修改',
    `name`        varchar(64)  NOT NULL COMMENT '角色名称',
    `description` varchar(255) DEFAULT NULL COMMENT '角色说明',
    `builtIn`     tinyint      NOT NULL DEFAULT 0 COMMENT '1 内置角色，不可删除',
    `status`      int          NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    `sortOrder`   int          NOT NULL DEFAULT 0,
    `createTime`  bigint       NOT NULL,
    `updateTime`  bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`code`),
    UNIQUE KEY `uk_sys_role_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `code`        varchar(64)  NOT NULL COMMENT '权限编码，如 admin:user',
    `name`        varchar(64)  NOT NULL,
    `groupName`   varchar(32)  NOT NULL COMMENT '权限分组，后台按组展示',
    `description` varchar(255) DEFAULT NULL,
    `sortOrder`   int          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_permission_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `roleId`       bigint NOT NULL,
    `permissionId` bigint NOT NULL,
    PRIMARY KEY (`roleId`, `permissionId`),
    KEY `idx_sys_role_permission_permission` (`permissionId`),
    CONSTRAINT `fk_sys_role_permission_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sys_role_permission_permission` FOREIGN KEY (`permissionId`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `userId` bigint NOT NULL,
    `roleId` bigint NOT NULL,
    PRIMARY KEY (`userId`, `roleId`),
    KEY `idx_sys_user_role_role` (`roleId`),
    CONSTRAINT `fk_sys_user_role_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sys_user_role_role` FOREIGN KEY (`roleId`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 权限点定义（代码中通过 PermissionCodes 常量引用，权限点本身由系统预置）
-- ---------------------------------------------------------------------------
INSERT INTO `sys_permission` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('admin:access', '后台访问', '基础', '进入管理后台，查看数据概览', 10),
       ('admin:user', '用户管理', '用户', '查看用户列表、重置用户密码', 20),
       ('admin:user:role', '用户角色分配', '用户', '为用户分配或移除角色', 21),
       ('admin:question', '问题管理', '内容', '删除/恢复问题、设置置顶', 30),
       ('admin:review', '内容审核', '内容', '审核问题与评论', 31),
       ('admin:search', '站内搜索', '内容', '后台搜索问题与用户', 32),
       ('admin:topic', '话题管理', '运营', '维护话题', 40),
       ('admin:classification', '分类管理', '运营', '维护问题分类与话题分类', 41),
       ('admin:advertisement', '广告与新闻', '运营', '维护广告投放', 42),
       ('admin:file', '文件管理', '系统', '查看与删除上传文件', 50),
       ('admin:storage', '存储配置', '系统', '维护本地/对象存储配置', 51),
       ('admin:system', '系统设置', '系统', '控制面板、系统配置、缓存与日志', 52),
       ('admin:role', '角色权限管理', '系统', '新增角色并配置角色权限', 53)
ON DUPLICATE KEY UPDATE `name`       = VALUES(`name`),
                        `groupName`  = VALUES(`groupName`),
                        `description`= VALUES(`description`),
                        `sortOrder`  = VALUES(`sortOrder`);

-- ---------------------------------------------------------------------------
-- 内置角色
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role` (`code`, `name`, `description`, `builtIn`, `status`, `sortOrder`, `createTime`, `updateTime`)
VALUES ('super_admin', '超级管理员', '系统内置角色，拥有全部权限且不可删除', 1, 1, 0,
        UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('content_admin', '内容审核员', '负责问题与评论审核', 0, 1, 10,
        UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('ops_admin', '运营管理员', '负责用户、话题、分类与广告运营', 0, 1, 20,
        UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`),
                        `updateTime`  = VALUES(`updateTime`);

-- 超级管理员：全部权限（内置角色权限由系统保证，后台不可编辑）
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         CROSS JOIN `sys_permission` p
WHERE r.`code` = 'super_admin';

-- 内容审核员
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN ('admin:access', 'admin:question', 'admin:review', 'admin:search')
WHERE r.`code` = 'content_admin';

-- 运营管理员
INSERT IGNORE INTO `sys_role_permission` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN ('admin:access', 'admin:user', 'admin:topic', 'admin:classification',
                              'admin:advertisement')
WHERE r.`code` = 'ops_admin';

-- ---------------------------------------------------------------------------
-- 存量管理员迁移：power = 0 的账号授予超级管理员角色，随后 power 回退为普通用户
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `sys_user_role` (`userId`, `roleId`)
SELECT up.`id`, r.`id`
FROM `userPermission` up
         CROSS JOIN `sys_role` r
WHERE up.`power` = 0
  AND r.`code` = 'super_admin';

UPDATE `userPermission` SET `power` = 1 WHERE `power` = 0;
