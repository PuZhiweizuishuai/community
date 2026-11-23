package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 吧主修改话题设置。
 */
@Data
public class TopicSettingsDTO {
    @Size(max = 500, message = "话题简介不能超过 500 个字符")
    private String simpleDesc;

    private String image;

    /** 0 公开，1 私有可申请，2 私有隐藏 */
    @Min(value = 0, message = "话题可见性不正确")
    @Max(value = 2, message = "话题可见性不正确")
    private Integer visibility;
}
