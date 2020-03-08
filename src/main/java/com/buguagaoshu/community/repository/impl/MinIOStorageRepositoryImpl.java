package com.buguagaoshu.community.repository.impl;

import com.buguagaoshu.community.config.MinIOConfigProperties;
import com.buguagaoshu.community.repository.FileStorageRepository;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-03-08 19:16
 */
@Repository
public class MinIOStorageRepositoryImpl implements FileStorageRepository {
    private final MinIOConfigProperties mcp;

    private final MinioClient minioClient;

    @Autowired
    public MinIOStorageRepositoryImpl(MinIOConfigProperties mcp, MinioClient minioClient) {
        this.mcp = mcp;
        this.minioClient = minioClient;
    }


    @Override
    public String getFileUrl(String filePath) {
        try {
            return minioClient.presignedGetObject(mcp.getBucket(), filePath, mcp.getOutTime());
        } catch (Exception e) {
            return "/image/404.png";
        }
    }

    @Override
    public String saveFile(String filePath, MultipartFile fileStream) {
        try {
            minioClient.putObject(mcp.getBucket(), filePath, fileStream.getInputStream(), fileStream.getSize(), fileStream.getContentType());
            return filePath;
        } catch (Exception e) {
            return "error";
        }
    }
}
