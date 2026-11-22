package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.util.StringUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 找回密码 / 重置密码 / 注册邮箱验证码请求体。
 */
public class EmailAuthDtos {

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        @NotBlank(message = "验证码不能为空")
        private String captcha;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "重置链接无效")
        private String token;
        @NotBlank(message = "新密码不能为空")
        @Pattern(regexp = StringUtil.PASSWORD_REGEX, message = StringUtil.PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Data
    public static class SendRegisterCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        @NotBlank(message = "验证码不能为空")
        private String captcha;
    }
}