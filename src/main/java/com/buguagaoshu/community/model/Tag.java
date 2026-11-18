package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 标签：用户发帖时创建，管理员审核通过后才公开显示。
 * status 语义与 ReviewStatusEnum 一致（0 待审核 / 1 已通过 / 2 未通过）。
 */
@Data
@Table(value = "tags", camelToUnderline = false)
public class Tag {
    @Id(keyType = KeyType.Auto)
    private long id;
    private String name;
    private int status;
    private long usageCount;
    private Long createdBy;
    private long createTime;
    private Long reviewerId;
    private Long reviewTime;
    private String rejectReason;
}
