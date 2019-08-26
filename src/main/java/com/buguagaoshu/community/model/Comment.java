package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 18:57
 */
@Data
public class Comment {
    /**
     * 评论 id
     * */
    private long commentId;

    /**
     * 评论所存在的问题
     * */
    private long questionId;

    /**
     * 评论目标 id
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

    /**
     * 创建时间
     * */
    private String createTime;

    /**
     * 修改时间
     * */
    private String modifiedTime;
}
