package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 13:34
 */
@Data
public class NotificationDTO {
    private long id;

    private String createTime;

    private int status;

    private long notifier;

    private long receiver;

    private String notifierId;

    private String notifierName;

    /**
     * 发起人头像地址（系统消息为空，前端展示场景图标）
     */
    private String notifierAvatar;

    /**
     * 发起人身份：user 表示真实用户（前端展示头像 + 昵称），
     * system 表示平台 / 管理员（前端展示「系统」徽章，不再拼昵称）
     */
    private String actorType;

    private String outerTitle;

    private long outerid;

    private long commentId = -1;

    private String typeName;

    private int type;

    /**
     * 缓存评论内容
     * */
    private String commentContent;

    private long commentCount;
    private long likeCount;
    private long systemCount;
}
