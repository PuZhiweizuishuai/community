package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申请加入话题。
 */
@Data
public class TopicJoinDTO {
    @Size(max = 200, message = "申请留言不能超过 200 个字符")
    private String message;
}
