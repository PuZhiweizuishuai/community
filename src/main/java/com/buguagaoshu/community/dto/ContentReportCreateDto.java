package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交举报请求。
 */
@Data
public class ContentReportCreateDto {

    @NotBlank(message = "举报对象类型不能为空")
    @Pattern(regexp = "question|comment", message = "举报对象类型不正确")
    private String targetType;

    @NotNull(message = "举报对象 ID 不能为空")
    @Positive(message = "举报对象 ID 不合法")
    private Long targetId;

    @NotBlank(message = "请选择举报理由")
    @Size(max = 32, message = "举报理由不合法")
    private String reason;

    @Size(max = 500, message = "补充说明不能超过 500 个字符")
    private String description;
}
