package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 17:09
 */
@Data
public class FollowUserDTO {
    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 不合法")
    private Long followUserId;

    private String token;
}
