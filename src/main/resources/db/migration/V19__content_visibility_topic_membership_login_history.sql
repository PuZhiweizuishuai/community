-- ===========================================================================
-- V19: 帖子登录可见 + 用户创建话题与私有话题 + 登录历史
--
-- 1. questions 增加 visibility（0 公开 / 1 登录可见）；
-- 2. topics 增加审核状态、创建人与可见性（0 公开 / 1 私有可申请 / 2 私有隐藏）；
-- 3. 新增 topic_members 保存加入申请与邀请；
-- 4. 新增 user_login_history 记录成功登录的时间、IP、UA 与入口；
-- 5. 新增权限点 content:topic:create 并授予内置角色。
--
-- 存量数据：questions 全部公开；topics 全部公开且视为审核通过；login history 为空。
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 帖子可见性
-- ---------------------------------------------------------------------------
ALTER TABLE `questions`
    ADD COLUMN `visibility` tinyint NOT NULL DEFAULT 0 COMMENT '0 公开，1 登录可见' AFTER `isEssence`;

CREATE INDEX `idx_questions_visibility` ON `questions` (`visibility`, `status`, `reviewStatus`);

-- ---------------------------------------------------------------------------
-- 话题审核与可见性
-- ---------------------------------------------------------------------------
ALTER TABLE `topics`
    ADD COLUMN `createdBy`    bigint       DEFAULT NULL COMMENT '创建人 userId，存量话题为 NULL',
    ADD COLUMN `reviewStatus` tinyint      NOT NULL DEFAULT 1 COMMENT '0 待审核，1 已通过，2 未通过',
    ADD COLUMN `reviewReason` varchar(200) DEFAULT NULL,
    ADD COLUMN `reviewerId`   bigint       DEFAULT NULL,
    ADD COLUMN `reviewTime`   bigint       DEFAULT NULL,
    ADD COLUMN `visibility`   tinyint      NOT NULL DEFAULT 0 COMMENT '0 公开，1 私有可申请，2 私有隐藏';

CREATE INDEX `idx_topics_review` ON `topics` (`reviewStatus`, `status`, `visibility`, `type`);
CREATE INDEX `idx_topics_creator` ON `topics` (`createdBy`, `id`);

-- ---------------------------------------------------------------------------
-- 话题成员：申请 / 邀请
-- 吧主与小吧只存在于 topic_moderators，不写入本表，避免双源不一致。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `topic_members`
(
    `id`         bigint       NOT NULL AUTO_INCREMENT,
    `topicId`    bigint       NOT NULL,
    `userId`     bigint       NOT NULL,
    `status`     tinyint      NOT NULL DEFAULT 0 COMMENT '0 待审核/待接受，1 已加入，2 已拒绝/已取消',
    `joinType`   varchar(16)  NOT NULL DEFAULT 'apply' COMMENT 'apply 申请，invite 邀请',
    `message`    varchar(200) DEFAULT NULL COMMENT '申请留言',
    `reviewerId` bigint       DEFAULT NULL,
    `reviewTime` bigint       DEFAULT NULL,
    `createTime` bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_members` (`topicId`, `userId`),
    KEY `idx_topic_members_topic` (`topicId`, `status`, `joinType`),
    KEY `idx_topic_members_user` (`userId`, `status`),
    CONSTRAINT `fk_topic_members_topic` FOREIGN KEY (`topicId`) REFERENCES `topics` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_topic_members_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 登录历史：仅记录成功登录
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_login_history`
(
    `id`        bigint       NOT NULL AUTO_INCREMENT,
    `userId`    bigint       NOT NULL,
    `loginTime` bigint       NOT NULL COMMENT '登录时间（毫秒时间戳）',
    `ip`        varchar(64)  NOT NULL DEFAULT '',
    `userAgent` varchar(512) DEFAULT NULL,
    `loginType` tinyint      NOT NULL DEFAULT 1 COMMENT '1 前台，2 后台',
    PRIMARY KEY (`id`),
    KEY `idx_login_history_user` (`userId`, `loginTime`, `id`),
    KEY `idx_login_history_time` (`loginTime`),
    CONSTRAINT `fk_login_history_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 权限点：用户创建话题（提交审核）
-- ---------------------------------------------------------------------------
INSERT INTO `sys_permissions` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('content:topic:create', '创建话题', '社区', '提交新话题申请，审核通过后成为吧主', 63)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `groupName`   = VALUES(`groupName`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`);

INSERT IGNORE INTO `sys_role_permissions` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_roles` r
         JOIN `sys_permissions` p ON p.`code` = 'content:topic:create'
WHERE r.`code` IN ('user', 'super_admin');
