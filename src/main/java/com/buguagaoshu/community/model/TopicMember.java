package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 话题成员关系：申请加入或邀请加入。
 * 吧主与小吧只存在于 topic_moderators，不写入本表。
 */
@Data
@Table(value = "topic_members", camelToUnderline = false)
public class TopicMember {
    @Id(keyType = KeyType.Auto)
    private long id;
    private long topicId;
    private long userId;
    /** 0 待审核/待接受，1 已加入，2 已拒绝/已取消 */
    private int status;
    /** apply 申请，invite 邀请 */
    private String joinType;
    private String message;
    private Long reviewerId;
    private Long reviewTime;
    private long createTime;
}
