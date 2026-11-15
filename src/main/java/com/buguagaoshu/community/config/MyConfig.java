package com.buguagaoshu.community.config;


import com.buguagaoshu.community.component.MyLocaleResolver;
import com.buguagaoshu.community.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.LocaleResolver;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 16:18
 * 自定义配置
 */
@Configuration
public class MyConfig {
    private final S3ConfigProperties s3Properties;

    @Autowired
    public MyConfig(S3ConfigProperties s3Properties) {
        this.s3Properties = s3Properties;
    }

    /**
     * 将 MyLocaleResolver 添加到容器
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocaleResolver();
    }


    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    private AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey()));
    }

    private URI endpointOverride() {
        String endpoint = s3Properties.getEndpoint();
        return (endpoint == null || endpoint.isBlank()) ? null : URI.create(endpoint.trim());
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(s3Properties.isPathStyleAccess())
                .build();
    }

    /**
     * S3 同步客户端，用于上传对象。
     * 显式使用 JDK 自带的 UrlConnectionHttpClient，避免额外引入 Apache/Netty HTTP 客户端。
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration())
                .httpClient(UrlConnectionHttpClient.builder().build());
        URI endpoint = endpointOverride();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }

    /**
     * S3 预签名器，用于生成带时效的文件访问链接。
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration());
        URI endpoint = endpointOverride();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }
}
