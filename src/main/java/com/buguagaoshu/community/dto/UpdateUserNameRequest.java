package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户名请求（表单）。
 */
@Data
public class UpdateUserNameRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名不能超过 20 个字符")
    private String newName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "请输入图形验证码")
    private String validateCode;
}
