package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.util.StringUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（application/x-www-form-urlencoded 表单）。
 * 字段级规则由 Bean Validation 声明，Controller 只保留验证码、频率等业务校验。
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户 ID 不能为空")
    @Pattern(regexp = "^\\w{3,20}$", message = "用户 ID 需为 3~20 位字母、数字或下划线")
    private String userId;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String userName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 30, message = "邮箱不能超过 30 个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = StringUtil.PASSWORD_REGEX, message = StringUtil.PASSWORD_MESSAGE)
    private String password;

    @Size(max = 2, message = "性别不合法")
    private String sex;

    /** 可选，空串视为未填写；填写时必须是 yyyy-MM-dd */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式不正确，需为 yyyy-MM-dd")
    private String birthday;

    @Size(max = 20, message = "学校名称不能超过 20 个字符")
    private String school;

    @Size(max = 20, message = "专业名称不能超过 20 个字符")
    private String major;

    @NotBlank(message = "请输入图形验证码")
    private String validateCode;

    /** 邮件服务启用时必填，是否必填取决于站点配置，保留在业务层判断 */
    private String mailCode;
}
