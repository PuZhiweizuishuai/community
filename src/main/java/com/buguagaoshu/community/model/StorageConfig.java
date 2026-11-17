package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 存储配置：id=0 固定为本地存储，id>=1 为 S3 兼容对象存储，可启用多个。
 */
@Data
@Table(value = "storage_config", camelToUnderline = false)
public class StorageConfig {
    public static final long LOCAL_ID = 0L;
    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_S3 = "s3";

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
    private String type;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private Boolean pathStyleAccess;
    private Long urlExpireSeconds;
    private Boolean enabled;
    private Long createTime;
    private Long modifiedTime;
}