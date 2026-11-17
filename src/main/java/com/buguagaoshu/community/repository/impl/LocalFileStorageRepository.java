package com.buguagaoshu.community.repository.impl;

import com.buguagaoshu.community.model.StorageConfig;
import com.buguagaoshu.community.repository.FileStorageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储，固定 id=0，根目录来自 File.ROOT.PATH。
 */
@Repository
public class LocalFileStorageRepository implements FileStorageRepository {

    private final Path root;

    public LocalFileStorageRepository(@Value("${File.ROOT.PATH}") String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public long storageId() {
        return StorageConfig.LOCAL_ID;
    }

    @Override
    public void save(String key, MultipartFile file) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public Resource load(String key) {
        Path target = resolve(key);
        return Files.isRegularFile(target) ? new FileSystemResource(target) : null;
    }

    @Override
    public boolean delete(String key) {
        try {
            return Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean preferRedirect() {
        return false;
    }

    @Override
    public String directUrl(String key) {
        return "/api/file/" + StorageConfig.LOCAL_ID + "/" + FileStorageRepository.normalizeKey(key);
    }

    /** 规范化后必须仍在存储根目录内，防止路径穿越 */
    private Path resolve(String key) {
        String normalized = FileStorageRepository.normalizeKey(key);
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }
}