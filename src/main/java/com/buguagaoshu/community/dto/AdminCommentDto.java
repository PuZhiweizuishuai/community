package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 后台评论管理列表项：包含评论正文、作者、所属问题与回复对象。
 */
@Data
public class AdminCommentDto {

    private long commentId;

    private long questionId;

    private String questionTitle;

    /** 所属问题状态：1 正常，0 已删除 */
    private int questionStatus;

    /** 所属问题审核状态：0 待审核，1 已通过，2 未通过 */
    private int questionReviewStatus;

    /** 根评论 ID；一级评论为 -1 */
    private long parentCommentId;

    /** 被回复的评论 ID；一级评论为问题 ID */
    private long parentId;

    /** 1 评论问题，2 回复评论 */
    private int type;

    private long authorId;

    private String authorUserId;

    private String authorName;

    private String authorHeadUrl;

    private String content;

    private long likeCount;

    private long commentCount;

    /** 1 正常，0 已删除 */
    private int status;

    /** 0 待审核，1 已通过，2 未通过 */
    private int reviewStatus;

    private String reviewReason;

    private String reviewSuggestion;

    private String createTime;

    private String modifiedTime;

    /** 被回复评论的作者昵称，仅二级回复返回 */
    private String parentAuthorName;
}
