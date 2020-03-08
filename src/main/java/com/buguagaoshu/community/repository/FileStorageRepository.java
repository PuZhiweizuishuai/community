package com.buguagaoshu.community.repository;

import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-03-08 19:14
 */
public interface FileStorageRepository {
    /**
     * @param filePath 文件保存路径
     * @return 返回文件链接
     * */
    String getFileUrl(String filePath);


    /**
     * @param filePath 文件保存路径
     * @param fileStream 文件流
     * @return 文件路径
     * */
    String saveFile(String filePath, MultipartFile fileStream);
}
