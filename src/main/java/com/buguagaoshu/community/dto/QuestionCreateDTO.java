package com.buguagaoshu.community.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布 / 编辑问题的请求体（application/json）。
 */
@Data
public class QuestionCreateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不能超过 50 个字符")
    private String title;
    @NotBlank(message = "请选择问题分类")
    private String classification;
    /**
     * 内容段落，1 ~ 3 个，按数组顺序展示
     */
    @Valid
    @NotEmpty(message = "输入内容不能为空")
    @Size(max = 3, message = "最多只能添加 3 个内容段落")
    private List<QuestionContentCreateDTO> contents;
    private String tag;
    private Long questionId = -1L;
    private Long createTime = 0L;
    private String captcha;
    /** 兼容旧表单字段名 CAPTCHA */
    private String CAPTCHA;

    public String getCaptchaValue() {
        return captcha != null && !captcha.isBlank() ? captcha : CAPTCHA;
    }
}
