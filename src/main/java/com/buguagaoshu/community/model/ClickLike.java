package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 15:59
 * 点赞
 */
@Data
public class ClickLike {
    private long likeId;
    private long notifier;
    private String notifierName;
    private long receiver;
    private long questionId;
    private long commentId;
    private int type;
    private long createTime;
}
