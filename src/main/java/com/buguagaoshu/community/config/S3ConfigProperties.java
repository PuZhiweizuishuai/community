package com.buguagaoshu.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3 兼容对象存储配置（AWS S3 / MinIO / 其他 S3 兼容服务）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@ConfigurationProperties(prefix = "s3")
@Component
@Data
public class S3ConfigProperties {
    /** 服务地址，例如 http://192.168.147.132:9000；留空则使用 AWS 默认端点 */
    private String endpoint = "";

    /** SigV4 签名使用的区域，S3 兼容服务一般填 us-east-1 */
    private String region = "us-east-1";

    private String accessKey;

    private String secretKey;

    private String bucket = "community";

    /** 预签名 URL 有效期，单位秒（AWS SigV4 上限为 7 天） */
    private long outTime = 86400L;

    /** MinIO 等自建服务需要 path-style 访问，AWS S3 可关闭 */
    private boolean pathStyleAccess = true;
}
