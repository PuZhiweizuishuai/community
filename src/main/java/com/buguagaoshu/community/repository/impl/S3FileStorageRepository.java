package com.buguagaoshu.community.repository.impl;

import com.buguagaoshu.community.model.StorageConfig;
import com.buguagaoshu.community.repository.FileStorageRepository;
import com.buguagaoshu.community.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * 一个 S3 兼容对象存储的读写实现，按 storage_config 表中的配置动态构建，
 * 由 StorageConfigRepository 缓存复用；配置变更后旧实例关闭重建。
 */
public class S3FileStorageRepository implements FileStorageRepository, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageRepository.class);

    /** AWS SigV4 预签名 URL 最长 7 天 */
    private static final long MAX_PRESIGN_SECONDS = 7L * 24 * 60 * 60;

    private final StorageConfig config;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3FileStorageRepository(StorageConfig config) {
        this.config = config;
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey()));
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(!Boolean.FALSE.equals(config.getPathStyleAccess()))
                // HTTP 端点默认启用 aws-chunked 分块载荷签名，部分 S3 兼容网关不解码，
                // 关闭后按 Content-Length 发送原始字节
                .chunkedEncodingEnabled(false)
                .build();
        URI endpoint = (config.getEndpoint() == null || config.getEndpoint().isBlank())
                ? null : URI.create(config.getEndpoint().trim());
        Region region = Region.of(config.getRegion() == null || config.getRegion().isBlank()
                ? "us-east-1" : config.getRegion().trim());

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(serviceConfig)
                // 默认 CRC32 校验和会启用 aws-chunked trailers，部分 S3 兼容网关不解码；
                // 仅在服务端要求时计算校验和，保证上传 body 为原始字节
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.builder().build());
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(serviceConfig);
        if (endpoint != null) {
            clientBuilder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }
        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    public StorageConfig config() {
        return config;
    }

    @Override
    public long storageId() {
        return config.getId();
    }

    @Override
    public void save(String key, MultipartFile file) throws IOException {
        String objectKey = FileStorageRepository.normalizeKey(key);
        // 关闭 chunked 后 SDK 要求流支持 mark/reset，直接按字节上传（单文件上限 10MB）
        byte[] bytes = file.getBytes();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(objectKey)
                .contentType(FileUtil.contentType(objectKey))
                .contentLength((long) bytes.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }

    @Override
    public Resource load(String key) {
        try {
            var response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(FileStorageRepository.normalizeKey(key))
                    .build());
            return new InputStreamResource(response);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(FileStorageRepository.normalizeKey(key))
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("删除 S3 对象失败，storageId={}, key={}: {}", config.getId(), key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean preferRedirect() {
        return true;
    }

    @Override
    public String directUrl(String key) {
        long expire = config.getUrlExpireSeconds() == null ? 86400L : config.getUrlExpireSeconds();
        long seconds = Math.min(Math.max(expire, 1L), MAX_PRESIGN_SECONDS);
        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(seconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(config.getBucket())
                        .key(FileStorageRepository.normalizeKey(key))
                        .build())
                .build());
        return presigned.url().toExternalForm();
    }

    /** 连接测试：bucket 存在且凭据有权限即通过 */
    public void testConnection() {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(config.getBucket()).build());
    }

    @Override
    public void close() {
        try {
            s3Client.close();
        } catch (Exception ignored) {
        }
        try {
            s3Presigner.close();
        } catch (Exception ignored) {
        }
    }
}