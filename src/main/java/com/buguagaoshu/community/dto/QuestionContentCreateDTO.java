package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发布/编辑问题时的单个内容段落
 */
@Data
public class QuestionContentCreateDTO {
    /**
     * 0 普通文本 / 1 密码可见 / 2 回复可见
     */
    private int type;
    @NotBlank(message = "段落内容不能为空")
    private String content;
    /**
     * 明文阅读密码，仅 type=1 时必填
     */
    private String password;
}