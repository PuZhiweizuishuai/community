package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 评论详情中的所属问题摘要，用于管理员查看上下文。
 */
@Data
public class AdminCommentQuestionDto {

    private long questionId;

    private String title;

    private Integer classification;

    private String tag;

    /** 1 正常，0 已删除 */
    private int status;

    /** 0 待审核，1 已通过，2 未通过 */
    private int reviewStatus;

    private long authorId;

    private String authorUserId;

    private String authorName;

    private String createTime;
}
