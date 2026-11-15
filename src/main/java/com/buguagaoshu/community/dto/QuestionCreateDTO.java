package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 发布 / 编辑问题的请求体（application/json）。
 */
@Data
public class QuestionCreateDTO {
    private String title;
    private String classification;
    private String description;
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
