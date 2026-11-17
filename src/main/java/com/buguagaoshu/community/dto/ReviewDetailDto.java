package com.buguagaoshu.community.dto;

import lombok.Data;

import java.util.List;

/**
 * 审核详情：评论只返回基础信息，问题额外返回全部内容段落（管理员可见明文）。
 */
@Data
public class ReviewDetailDto {
    private ReviewItemDto item;

    private List<QuestionContentDto> contents;
}
