package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 内容审核流水：提交 / 通过 / 驳回均写入一条记录，便于追溯审核过程。
 */
@Data
@Table(value = "content_review_log", camelToUnderline = false)
public class ContentReviewLog {
    @Id(keyType = KeyType.Auto)
    private long id;

    /**
     * 审核对象类型：question / comment
     */
    private String targetType;

    /**
     * 问题 ID 或评论 ID
     */
    private long targetId;

    /**
     * 动作，见 ReviewActionEnum
     */
    private int action;

    /**
     * 操作人：提交人为作者，通过/驳回为审核管理员
     */
    private long operatorId;

    /**
     * 驳回原因
     */
    private String reason;

    /**
     * 修改意见
     */
    private String suggestion;

    private long createTime;
}
