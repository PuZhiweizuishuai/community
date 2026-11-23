package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邀请用户加入话题。
 */
@Data
public class TopicInviteDTO {
    @NotBlank(message = "用户账号不能为空")
    private String userCode;
}
