package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:07
 */
@Data
public class FollowTopicDTO {
    @NotNull(message = "话题 ID 不能为空")
    @Positive(message = "话题 ID 不合法")
    private Long topicId;

    @NotBlank(message = "请选择要关注的话题")
    @Size(max = 50, message = "话题名称不能超过 50 个字符")
    private String topicTitle;

    private String token;
}
