package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 单条广告投放操作的结果，用于批量接口的逐条回执。
 */
@Data
public class AdvertisementOperationResult {
    private Long id;
    private String title;
    private boolean success;
    private String message;
    /** 兼容旧接口的返回码：1 成功、2 位置已满、-1 广告不存在、3 未投放、0 参数错误 */
    private int code;

    public static AdvertisementOperationResult of(Long id, String title, int code, String message) {
        AdvertisementOperationResult result = new AdvertisementOperationResult();
        result.setId(id);
        result.setTitle(title);
        result.setCode(code);
        result.setSuccess(code == 1);
        result.setMessage(message);
        return result;
    }
}
