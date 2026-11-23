package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户申请创建话题。
 */
@Data
public class TopicCreateDTO {
    @NotBlank(message = "话题名称不能为空")
    @Size(max = 50, message = "话题名称不能超过 50 个字符")
    private String title;

    @NotNull(message = "请选择话题分类")
    @Positive(message = "请选择话题分类")
    private Integer type;

    @Size(max = 500, message = "话题简介不能超过 500 个字符")
    private String simpleDesc;

    private String image;

    /** 0 公开，1 私有可申请，2 私有隐藏 */
    @Min(value = 0, message = "话题可见性不正确")
    @Max(value = 2, message = "话题可见性不正确")
    private Integer visibility = 0;

    private String captcha;
}
