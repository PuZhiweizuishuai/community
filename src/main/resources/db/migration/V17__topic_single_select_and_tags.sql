-- ===========================================================================
-- V17: 话题单选（贴吧化）+ 标签多选（带审核）+ 话题吧务
--
-- 1. 新增 tags / question_tags / topic_moderators 三张表；
-- 2. questions 增加 topicId、isTop、isEssence，classification 由名称改为 code；
-- 3. 新增 admin:tag 权限点；
-- 4. 存量数据回填由 TopicTagBackfillRunner 在应用启动时幂等执行
--    （拆分旧 tag 串、补齐 topicId、生成标签关联、重算计数，
--     最后把 topicId 收紧为 NOT NULL 并补外键）。
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 标签
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tags`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `name`         varchar(50)  NOT NULL,
    `status`       int          NOT NULL DEFAULT 0 COMMENT '0 待审核，1 已通过，2 未通过（与 ReviewStatusEnum 一致）',
    `usageCount`   bigint       NOT NULL DEFAULT 0,
    `createdBy`    bigint       DEFAULT NULL COMMENT '创建人 userId，存量迁移为 NULL',
    `createTime`   bigint       NOT NULL,
    `reviewerId`   bigint       DEFAULT NULL,
    `reviewTime`   bigint       DEFAULT NULL,
    `rejectReason` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tags_name` (`name`),
    KEY `idx_tags_status` (`status`, `usageCount`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 帖子 - 标签关联
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_tags`
(
    `questionId` bigint NOT NULL,
    `tagId`      bigint NOT NULL,
    PRIMARY KEY (`questionId`, `tagId`),
    KEY `idx_question_tags_tag` (`tagId`),
    CONSTRAINT `fk_qt_question` FOREIGN KEY (`questionId`) REFERENCES `questions` (`questionId`) ON DELETE CASCADE,
    CONSTRAINT `fk_qt_tag` FOREIGN KEY (`tagId`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 话题吧务
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `topic_moderators`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `topicId`    bigint      NOT NULL,
    `userId`     bigint      NOT NULL,
    `role`       varchar(20) NOT NULL DEFAULT 'moderator' COMMENT 'owner=吧主 moderator=小吧',
    `assignedBy` bigint      NOT NULL COMMENT '任命人 userId',
    `createTime` bigint      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_moderators` (`topicId`, `userId`),
    KEY `idx_topic_moderators_user` (`userId`),
    CONSTRAINT `fk_tm_topic` FOREIGN KEY (`topicId`) REFERENCES `topics` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tm_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- questions 变更
-- ---------------------------------------------------------------------------
ALTER TABLE `questions`
    ADD COLUMN `topicId`   bigint DEFAULT NULL AFTER `classification`,
    ADD COLUMN `isTop`     int    NOT NULL DEFAULT 0 COMMENT '话题内置顶',
    ADD COLUMN `isEssence` int    NOT NULL DEFAULT 0 COMMENT '加精';

CREATE INDEX `idx_questions_topic` ON `questions` (`topicId`, `status`, `reviewStatus`, `alterTime`);

-- ---------------------------------------------------------------------------
-- 分类种子：保证 code 转换在从未启动过新版应用的老库上也能正确匹配
-- 先统一排序规则，避免 classifications（运行时建表默认 0900_ai_ci）与
-- questions（utf8mb4_unicode_ci）比较时出现 Illegal mix of collations
-- ---------------------------------------------------------------------------
ALTER TABLE `classifications` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT IGNORE INTO `classifications` (`kind`, `code`, `name`, `sortOrder`, `status`, `createTime`, `modifiedTime`)
VALUES ('question', 1, '提问', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('question', 2, '讨论', 2, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('question', 3, '分享', 3, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('question', 4, '建议', 4, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('question', 5, 'Bug', 5, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('question', 6, '灌水', 6, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 1, '开发语言', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 2, '平台框架', 2, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 3, '服务器', 3, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 4, '数据库', 4, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 5, '开发工具', 5, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 6, '系统设备', 6, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
       ('topic', 7, '休闲灌水', 7, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ---------------------------------------------------------------------------
-- 默认话题「综合讨论」：存量回填时无归属的帖子统一归入该话题
-- ---------------------------------------------------------------------------
INSERT INTO `topics` (`title`, `talkCount`, `followCount`, `image`, `simpleDesc`, `type`, `createTime`, `modifiedTime`, `status`)
SELECT '综合讨论', 0, 0, '/image/topicicon/default.svg', '暂时没有归属话题的讨论都会放在这里', 7,
       UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 1
WHERE NOT EXISTS (SELECT 1 FROM `topics` WHERE `title` = '综合讨论');

-- ---------------------------------------------------------------------------
-- classification 名称 → code
-- ---------------------------------------------------------------------------
UPDATE `questions` q
    LEFT JOIN `classifications` c
        ON c.`kind` = 'question' AND c.`name` = q.`classification` COLLATE utf8mb4_unicode_ci
SET q.`classification` = CAST(COALESCE(c.`code`, 1) AS CHAR);

ALTER TABLE `questions`
    MODIFY COLUMN `classification` int NOT NULL DEFAULT 1 COMMENT '问题类型 code（classifications.kind=question）';

-- ---------------------------------------------------------------------------
-- 权限点：后台标签管理
-- ---------------------------------------------------------------------------
INSERT INTO `sys_permissions` (`code`, `name`, `groupName`, `description`, `sortOrder`)
VALUES ('admin:tag', '标签管理', '内容', '审核、重命名与删除标签', 33)
ON DUPLICATE KEY UPDATE `name`        = VALUES(`name`),
                        `groupName`   = VALUES(`groupName`),
                        `description` = VALUES(`description`),
                        `sortOrder`   = VALUES(`sortOrder`);

INSERT IGNORE INTO `sys_role_permissions` (`roleId`, `permissionId`)
SELECT r.`id`, p.`id`
FROM `sys_roles` r
         JOIN `sys_permissions` p ON p.`code` = 'admin:tag'
WHERE r.`code` IN ('super_admin', 'content_admin');
