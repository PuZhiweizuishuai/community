package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 后台举报列表项。
 */
@Data
public class ContentReportDto {

    private long id;

    private String targetType;

    private String targetTypeName;

    private long targetId;

    private long questionId;

    private String questionTitle;

    private int questionStatus;

    /** 被举报内容当前状态：1 正常，0 已删除 */
    private int targetStatus;

    /** 被举报内容当前审核状态：0 待审核，1 已通过，2 未通过 */
    private int targetReviewStatus;

    private String targetSnapshot;

    private long targetUserId;

    private String targetAuthorUserId;

    private String targetAuthorName;

    private long reporterId;

    private String reporterUserId;

    private String reporterName;

    private String reason;

    private String reasonName;

    private String description;

    /** 0 待处理，1 已受理，2 已驳回 */
    private int status;

    private String statusName;

    private long handlerId;

    private String handlerName;

    private String handleNote;

    private int contentRemoved;

    private String createTime;

    private String handleTime;
}
