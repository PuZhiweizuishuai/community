package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 问题详情/列表展示用的标签。status 用于前端给作者本人展示「待审核」徽标。
 */
@Data
public class QuestionTagDto {
    private long id;
    private String name;
    private int status;
}
