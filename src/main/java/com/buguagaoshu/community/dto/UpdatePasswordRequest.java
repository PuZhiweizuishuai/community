package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求（表单，兼容前端使用的 CAPTCHA 字段名）。
 */
@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = StringUtil.PASSWORD_REGEX, message = StringUtil.PASSWORD_MESSAGE)
    private String newPassword;

    @NotBlank(message = "请输入图形验证码")
    private String CAPTCHA;
}
