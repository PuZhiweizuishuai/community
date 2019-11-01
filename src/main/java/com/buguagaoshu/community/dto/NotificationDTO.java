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
