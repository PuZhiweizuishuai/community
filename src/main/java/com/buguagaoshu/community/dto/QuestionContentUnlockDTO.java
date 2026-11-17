package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 密码可见段落的解锁请求
 */
@Data
public class QuestionContentUnlockDTO {
    @Min(value = 1, message = "内容段落 ID 不合法")
    private long contentId;
    private String password;
}