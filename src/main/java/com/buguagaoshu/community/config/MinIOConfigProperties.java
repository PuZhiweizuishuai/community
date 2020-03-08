package com.buguagaoshu.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-03-08 19:09
 */
@ConfigurationProperties(prefix = "minio")
@Component
@Data
public class MinIOConfigProperties {
    private String server;
    private String accessKey;
    private String secretKey;
    private Integer outTime;
    private String bucket;
}
