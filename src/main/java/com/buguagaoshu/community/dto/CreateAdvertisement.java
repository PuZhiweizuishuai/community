package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 23:05
 */
@Data
public class CreateAdvertisement {
    private Long id;

    @NotBlank(message = "广告标题不能为空")
    @Size(max = 256, message = "广告标题不能超过 256 个字符")
    private String title;

    @NotBlank(message = "广告跳转链接不能为空")
    @Size(max = 1000, message = "广告跳转链接不能超过 1000 个字符")
    private String url;

    @NotBlank(message = "广告图片不能为空")
    @Size(max = 1000, message = "广告图片地址不能超过 1000 个字符")
    private String image;

    private Long time;
    private String position;
}
