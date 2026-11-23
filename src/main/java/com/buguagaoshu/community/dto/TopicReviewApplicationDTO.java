package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 吧主团队审核加入申请。
 */
@Data
public class TopicReviewApplicationDTO {
    @NotNull(message = "请选择审核结果")
    private Boolean approve;

    @Size(max = 200, message = "审核意见不能超过 200 个字符")
    private String reason;
}
