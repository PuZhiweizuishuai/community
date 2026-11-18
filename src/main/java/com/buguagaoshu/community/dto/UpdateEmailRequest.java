package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改邮箱请求（表单）。
 */
@Data
public class UpdateEmailRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 30, message = "邮箱不能超过 30 个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "请输入图形验证码")
    private String validateCode;
}
