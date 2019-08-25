package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:22
 */
@Data
public class CommentDto {
    /**
     * 评论 id
     * */
    private long commentId;

    /**
     * 问题 id
     * */
    private long parentId;

    /**
     * 评论类型
     * */
    private int type;

    /**
     * 评论人
     * */
    private long commentator;

    /**
     * 内容
     * */
    private String content;

    /**
     * 点赞数
     * */
    private long likeCount;
}
