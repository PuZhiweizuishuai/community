-- ===========================================================================
-- V11: 内容审核（问题 + 评论）
-- reviewStatus：0 待审核 / 1 已通过 / 2 未通过
-- 存量数据统一置为 1（已通过），保证开启审核前的内容继续可见；
-- 开关关闭时新内容也直接写 1，开启时写 0 并进入待审核区。
-- 审核状态与 status（删除标记）是两个独立维度，互不复用。
-- ===========================================================================

ALTER TABLE `Questions`
    ADD COLUMN `reviewStatus`     int          NOT NULL DEFAULT 1 COMMENT '审核状态：0 待审核，1 已通过，2 未通过',
    ADD COLUMN `reviewReason`     varchar(500) DEFAULT NULL COMMENT '审核不通过原因',
    ADD COLUMN `reviewSuggestion` varchar(500) DEFAULT NULL COMMENT '管理员修改意见',
    ADD COLUMN `reviewerId`       bigint       DEFAULT NULL COMMENT '最近一次审核的管理员',
    ADD COLUMN `reviewTime`       bigint       DEFAULT NULL COMMENT '最近一次审核时间';

ALTER TABLE `comment`
    ADD COLUMN `reviewStatus`     int          NOT NULL DEFAULT 1 COMMENT '审核状态：0 待审核，1 已通过，2 未通过',
    ADD COLUMN `reviewReason`     varchar(500) DEFAULT NULL COMMENT '审核不通过原因',
    ADD COLUMN `reviewSuggestion` varchar(500) DEFAULT NULL COMMENT '管理员修改意见',
    ADD COLUMN `reviewerId`       bigint       DEFAULT NULL COMMENT '最近一次审核的管理员',
    ADD COLUMN `reviewTime`       bigint       DEFAULT NULL COMMENT '最近一次审核时间';

-- 后台待审核列表：按审核状态 + 删除状态 + 主键倒序
CREATE INDEX `idx_questions_review` ON `Questions` (`reviewStatus`, `status`, `questionId`);
-- 用户「我的审核」列表：按作者 + 审核状态
CREATE INDEX `idx_questions_user_review` ON `Questions` (`userId`, `reviewStatus`, `questionId`);
CREATE INDEX `idx_comment_review` ON `comment` (`reviewStatus`, `status`, `commentId`);
CREATE INDEX `idx_comment_user_review` ON `comment` (`commentator`, `reviewStatus`, `commentId`);

-- 审核流水：提交 / 通过 / 驳回均留痕，便于追溯审核过程
CREATE TABLE `content_review_log`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `targetType`  varchar(20) NOT NULL COMMENT 'question / comment',
    `targetId`    bigint      NOT NULL COMMENT '问题 ID 或评论 ID',
    `action`      int         NOT NULL COMMENT '1 提交审核，2 审核通过，3 审核驳回',
    `operatorId`  bigint      NOT NULL COMMENT '操作人（提交人或审核管理员）',
    `reason`      varchar(500) DEFAULT NULL COMMENT '驳回原因',
    `suggestion`  varchar(500) DEFAULT NULL COMMENT '修改意见',
    `createTime`  bigint      NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_review_log_target` (`targetType`, `targetId`, `id`),
    KEY `idx_review_log_action` (`action`, `createTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 内容审核开关：默认关闭，管理员在「后台 → 控制面板 → 系统配置」中开启
INSERT IGNORE INTO system_config (configKey, configValue, modifiedTime)
VALUES ('review.enabled', 'false', NULL);
