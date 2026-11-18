package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    /** 兼容旧前端；新前端只传 topicId，标题由服务端按 ID 读取 */
    private String topicTitle;

    private String token;
}
