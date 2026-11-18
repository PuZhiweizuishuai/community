package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求（表单）。
 * 长度上限与 users 表字段一致；未传（null）的字段保持原值。
 */
@Data
public class UpdateProfileRequest {
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String userName;

    @Size(max = 2, message = "性别不合法")
    private String sex;

    @Size(max = 20, message = "学校名称不能超过 20 个字符")
    private String school;

    @Size(max = 20, message = "专业名称不能超过 20 个字符")
    private String major;

    @Size(max = 50, message = "个性签名不能超过 50 个字符")
    private String simpleSelfIntroduction;

    @Size(max = 500, message = "简介不能超过 500 个字符")
    private String selfIntroduction;

    @Size(max = 50, message = "爱好不能超过 50 个字符")
    private String like;
}
