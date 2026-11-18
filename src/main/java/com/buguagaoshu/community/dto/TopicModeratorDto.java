package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 话题吧务成员展示信息。
 */
@Data
public class TopicModeratorDto {
    private long id;
    private long userId;
    private String userCode;
    private String userName;
    private String headUrl;
    private String role;
    private long createTime;
}
