package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 审核列表项：问题与评论共用，type 区分具体对象。
 */
@Data
public class ReviewItemDto {
    /**
     * 问题 ID 或评论 ID
     */
    private long id;

    /**
     * question / comment
     */
    private String type;

    /**
     * 评论所属问题 ID；问题本身也返回自身 ID，便于前端统一处理
     */
    private long questionId;

    private String questionTitle;

    /**
     * 列表标题：问题标题或评论摘要
     */
    private String title;

    /**
     * 评论原文（问题为空，问题正文走详情接口）
     */
    private String content;

    private long authorId;

    private String authorUserId;

    private String authorName;

    private String authorHeadUrl;

    private int reviewStatus;

    private int status;

    private String createTime;

    private String reviewTime;

    private String reviewReason;

    private String reviewSuggestion;

    private Integer classification;

    private String tag;

    private String reviewerName;
}
