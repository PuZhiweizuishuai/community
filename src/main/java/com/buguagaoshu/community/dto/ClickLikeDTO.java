package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:13
 */
@Data
public class ClickLikeDTO {
    private Long notifier;
    private String notifierName;
    private Long receiver;

    @NotNull(message = "问题 ID 不能为空")
    @Positive(message = "问题 ID 不合法")
    private Long questionId;

    /** -1 或空表示点赞问题本身，否则为评论 ID */
    private Long commentId;
    private String token;
}
