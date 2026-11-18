package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 内容举报记录：保存举报对象、举报时快照与处理结果，便于后续追溯。
 */
@Data
@Table(value = "content_reports", camelToUnderline = false)
public class ContentReport {

    @Id(keyType = KeyType.Auto)
    private long id;

    /** question / comment */
    private String targetType;

    private long targetId;

    /** 评论举报时所属问题；问题举报时等于 targetId */
    private long questionId;

    /** 被举报内容作者 */
    private long targetUserId;

    /** 举报时的内容快照，避免原内容被修改或删除后无法回溯 */
    private String targetSnapshot;

    private long reporterId;

    /** spam / abuse / porn / illegal / other */
    private String reason;

    private String description;

    /** 0 待处理，1 已受理，2 已驳回 */
    private int status;

    private long handlerId;

    private String handleNote;

    private long handleTime;

    /** 受理时是否同步删除被举报内容 */
    private int contentRemoved;

    private long createTime;
}
