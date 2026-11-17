-- 文件存储重构：存储配置表 + 文件记录表，并把历史本地文件链接迁移到统一 /api/file/0/ 前缀

CREATE TABLE IF NOT EXISTS storage_config
(
    id               bigint PRIMARY KEY AUTO_INCREMENT,
    name             varchar(64)  NOT NULL,
    -- local: 本地磁盘（固定 id=0）；s3: S3 兼容对象存储
    type             varchar(16)  NOT NULL DEFAULT 's3',
    endpoint         varchar(255) DEFAULT NULL,
    region           varchar(64)  DEFAULT 'us-east-1',
    accessKey        varchar(255) DEFAULT NULL,
    secretKey        varchar(255) DEFAULT NULL,
    bucket           varchar(128) DEFAULT NULL,
    pathStyleAccess  tinyint      DEFAULT 1,
    -- 预签名 URL 有效期（秒），AWS SigV4 上限 7 天
    urlExpireSeconds bigint       DEFAULT 86400,
    enabled          tinyint      NOT NULL DEFAULT 1,
    createTime       bigint       DEFAULT NULL,
    modifiedTime     bigint       DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 本地存储固定 id=0，默认启用；对象存储从 id=1 开始
INSERT IGNORE INTO storage_config (id, name, type, enabled, createTime)
VALUES (0, '本地存储', 'local', 1, UNIX_TIMESTAMP() * 1000);

CREATE TABLE IF NOT EXISTS file_record
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    storageId    bigint       NOT NULL DEFAULT 0,
    -- 存储内的相对键，如 MJM/image/xxx.jpg
    fileKey      varchar(500) NOT NULL,
    originalName varchar(255) DEFAULT NULL,
    fileSize     bigint       DEFAULT NULL,
    contentType  varchar(128) DEFAULT NULL,
    -- image / video / audio / pdf / other
    fileType     varchar(16)  DEFAULT NULL,
    url          varchar(600) DEFAULT NULL,
    uploaderId   bigint       DEFAULT NULL,
    -- 0 正常；1 已删除（物理文件已移除，记录保留供审计）
    status       tinyint      NOT NULL DEFAULT 0,
    createTime   bigint       DEFAULT NULL,
    UNIQUE KEY uk_storage_key (storageId, fileKey(191)),
    KEY idx_file_type (fileType),
    KEY idx_uploader (uploaderId),
    KEY idx_create_time (createTime)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 历史数据均为本地存储：/file/{userId}/... -> /api/file/0/{userId}/...
-- 单值 URL 列：仅改写以 /file/ 开头的值，避免误伤 /image/ 默认图等静态资源
UPDATE users SET headUrl = REPLACE(headUrl, '/file/', '/api/file/0/') WHERE headUrl LIKE '/file/%';
UPDATE users SET userTopPhotoUrl = REPLACE(userTopPhotoUrl, '/file/', '/api/file/0/') WHERE userTopPhotoUrl LIKE '/file/%';
UPDATE topic SET image = REPLACE(image, '/file/', '/api/file/0/') WHERE image LIKE '/file/%';
UPDATE advertisement SET image = REPLACE(image, '/file/', '/api/file/0/') WHERE image LIKE '/file/%';

-- 富文本/多值列：内容中可能内嵌 markdown 图片链接；已含新前缀的行跳过，防止重复替换
UPDATE Questions SET fileUrl = REPLACE(fileUrl, '/file/', '/api/file/0/')
WHERE fileUrl LIKE '%/file/%' AND fileUrl NOT LIKE '%/api/file/%';
UPDATE Questions SET description = REPLACE(description, '/file/', '/api/file/0/')
WHERE description LIKE '%/file/%' AND description NOT LIKE '%/api/file/%';
UPDATE comment SET content = REPLACE(content, '/file/', '/api/file/0/')
WHERE content LIKE '%/file/%' AND content NOT LIKE '%/api/file/%';
UPDATE notification SET commentContent = REPLACE(commentContent, '/file/', '/api/file/0/')
WHERE commentContent LIKE '%/file/%' AND commentContent NOT LIKE '%/api/file/%';