package com.buguagaoshu.community.repository.impl;

import com.buguagaoshu.community.config.S3ConfigProperties;
import com.buguagaoshu.community.repository.FileStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * 基于 AWS SDK v2 的 S3 对象存储实现，可对接 AWS S3 及 MinIO 等 S3 兼容服务。
 * 替代原 io.minio 3.0.10 实现，接口契约保持不变。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Repository
public class S3StorageRepositoryImpl implements FileStorageRepository {

    private static final Logger log = LoggerFactory.getLogger(S3StorageRepositoryImpl.class);

    /** AWS SigV4 预签名 URL 最长 7 天 */
    private static final long MAX_PRESIGN_SECONDS = 7L * 24 * 60 * 60;

    private final S3ConfigProperties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageRepositoryImpl(S3ConfigProperties properties,
                                   S3Client s3Client,
                                   S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String getFileUrl(String filePath) {
        String key = normalizeKey(filePath);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            long seconds = Math.min(Math.max(properties.getOutTime(), 1L), MAX_PRESIGN_SECONDS);
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(seconds))
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toExternalForm();
        } catch (Exception e) {
            log.warn("生成 S3 预签名链接失败，key={}: {}", key, e.getMessage());
            return "/image/404.png";
        }
    }

    @Override
    public String saveFile(String filePath, MultipartFile fileStream) {
        String key = normalizeKey(filePath);
        long size = fileStream.getSize();
        try (InputStream in = fileStream.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(fileStream.getContentType())
                    .contentLength(size)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(in, size));
            return filePath;
        } catch (Exception e) {
            log.error("上传文件到 S3 失败，key={}: {}", key, e.getMessage());
            return "error";
        }
    }

    /**
     * 原代码传入的对象名带有前导 "/"，S3 中会造成空前缀目录，统一去掉。
     */
    private String normalizeKey(String path) {
        if (path == null) {
            return "";
        }
        String key = path.replace('\\', '/');
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }
}
