package com.buguagaoshu.community.dto;

import lombok.Data;

import java.util.List;

/**
 * 后台问题详情：问题本身（含作者、审核与统计信息） + 全部正文段落。
 * 管理员可见全部段落，包括「密码可见」「回复可见」内容，便于判断是否违规。
 */
@Data
public class AdminQuestionDetailDto {

    private QuestionDto question;

    private List<QuestionContentDto> contents;
}
