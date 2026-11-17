-- 系统配置键值表：注册开关、SMTP 邮件配置等后台可编辑设置项
CREATE TABLE IF NOT EXISTS system_config
(
    configKey    varchar(64) NOT NULL,
    configValue  text,
    modifiedTime bigint DEFAULT NULL,
    PRIMARY KEY (configKey)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO system_config (configKey, configValue, modifiedTime)
VALUES ('register.enabled', 'true', NULL),
       ('mail.enabled', 'false', NULL),
       ('mail.host', '', NULL),
       ('mail.port', '465', NULL),
       ('mail.username', '', NULL),
       ('mail.password', '', NULL),
       ('mail.from', '', NULL),
       ('mail.ssl', 'true', NULL);