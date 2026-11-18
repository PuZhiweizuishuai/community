package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 14:54
 */
@Data
public class FollowQuestionDTO {
    @NotNull(message = "问题 ID 不能为空")
    @Positive(message = "问题 ID 不合法")
    private Long followQuestionId;

    private String Token;
}
