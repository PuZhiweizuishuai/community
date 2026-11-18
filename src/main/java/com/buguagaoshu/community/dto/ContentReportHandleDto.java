package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台处理举报请求。
 *
 * @param accepted      是否受理：true 受理，false 驳回
 * @param note          处理说明；驳回时必填
 * @param deleteContent 受理时是否同步删除被举报内容
 */
public record ContentReportHandleDto(
        @NotNull(message = "请选择处理结果") Boolean accepted,
        @Size(max = 500, message = "处理说明不能超过 500 个字符") String note,
        Boolean deleteContent) {
}
