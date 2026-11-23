package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 话题成员 / 申请 / 邀请展示对象。
 */
@Data
public class TopicMemberDto {
    private long id;
    private long topicId;
    private long userId;
    private String userCode;
    private String userName;
    private String headUrl;
    private int status;
    private String joinType;
    private String message;
    private long createTime;
}
