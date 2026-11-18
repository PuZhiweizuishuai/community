package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 举报详情：举报记录 + 所属问题 + 评论举报的评论上下文。
 */
@Data
public class ContentReportDetailDto {

    private ContentReportDto report;

    private AdminCommentQuestionDto question;

    /** 评论举报时返回评论及其父 / 根评论上下文；问题举报为 null */
    private AdminCommentDetailDto commentDetail;
}
