package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 后台标签列表行。
 */
@Data
public class TagAdminDto {
    private long id;
    private String name;
    private int status;
    private long usageCount;
    private Long createdBy;
    private String creatorName;
    private long createTime;
    private Long reviewerId;
    private String reviewerName;
    private Long reviewTime;
    private String rejectReason;
}
