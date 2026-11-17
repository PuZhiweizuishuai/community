package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 前后端分离后的登录请求体（application/json）。
 */
@Data
public class LoginRequest {
    @NotBlank(message = "邮箱不能为空")
    private String email;
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 兼容旧前端字段名 validateCode */
    private String captcha;
    private String validateCode;
    private Boolean remember;

    public String getCaptchaValue() {
        return captcha != null && !captcha.isBlank() ? captcha : validateCode;
    }

    public String getRememberValue() {
        return Boolean.TRUE.equals(remember) ? "remember-me" : null;
    }
}
