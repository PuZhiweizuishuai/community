package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 文件记录：每次上传落库一条，url 为统一访问地址 /api/file/{storageId}/{fileKey}。
 */
@Data
@Table(value = "file_records", camelToUnderline = false)
public class FileRecord {
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_DELETED = 1;

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long storageId;
    private String fileKey;
    private String originalName;
    private Long fileSize;
    private String contentType;
    private String fileType;
    private String url;
    private Long uploaderId;
    private Integer status;
    private Long createTime;
}