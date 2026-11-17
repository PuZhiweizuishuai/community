package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 文件管理页展示用文件记录，附带存储名与上传者名。
 */
@Data
public class FileRecordDto {
    private Long id;
    private Long storageId;
    private String storageName;
    private String fileKey;
    private String originalName;
    private Long fileSize;
    private String contentType;
    private String fileType;
    private String url;
    private Long uploaderId;
    private String uploaderName;
    private Integer status;
    private Long createTime;
}