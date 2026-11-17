package com.buguagaoshu.community.repository;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 单个存储后端的读写抽象：本地磁盘（id=0）或一个 S3 兼容对象存储（id>=1）。
 * key 为存储内相对路径，如 MJM/image/xxx.jpg，不带前导斜杠。
 */
public interface FileStorageRepository {

    long storageId();

    void save(String key, MultipartFile file) throws IOException;

    /** 读取文件内容；不存在返回 null */
    Resource load(String key);

    boolean delete(String key);

    /** 是否以重定向方式读取（对象存储走预签名 URL，本地直接流式输出） */
    boolean preferRedirect();

    /** 直连地址：本地为统一 API 路径，对象存储为预签名 URL */
    String directUrl(String key);

    /**
     * 归一化对象键：去掉前导斜杠与 "." / ".." 段，防止路径穿越到非预期前缀。
     */
    static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        String path = key.replace('\\', '/');
        StringBuilder safe = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                continue;
            }
            if (safe.length() > 0) {
                safe.append('/');
            }
            safe.append(segment);
        }
        return safe.toString();
    }
}