package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 后台评论详情：评论本身 + 所属问题 + 被回复评论 / 根评论上下文。
 */
@Data
public class AdminCommentDetailDto {

    private AdminCommentDto comment;

    private AdminCommentQuestionDto question;

    /** 被直接回复的评论；一级评论为 null */
    private AdminCommentDto parentComment;

    /** 回复所属的根评论；仅当回复的是二级评论时返回 */
    private AdminCommentDto rootComment;
}
