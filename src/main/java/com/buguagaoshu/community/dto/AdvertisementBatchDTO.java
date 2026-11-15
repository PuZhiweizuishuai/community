package com.buguagaoshu.community.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 广告批量投放 / 批量结束请求体。
 */
@Data
public class AdvertisementBatchDTO {
    private List<Long> ids = new ArrayList<>();
    /** 投放位置，取值见 AdvertisementCache 常量 */
    private String position;
    /** 投放天数（1 ~ 365） */
    private Long time;
}
