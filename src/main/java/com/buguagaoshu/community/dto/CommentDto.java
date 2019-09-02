package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.model.User;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-28 22:12
 * 向用户发送评论数据
 */
@Data
public class CommentDto {
    /**
     * 评论 id
     */
    private long commentId;

    /**
     * 评论所存在的问题
     */
    private long questionId;

    /**
     * 评论目标 id
     */
    private long parentId;

    /**
     * 评论类型
     */
    private int type;

    /**
     * 评论人
     */
    private long commentator;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private long likeCount;

    /**
     * 评论回复数
     */
    private long commentCount;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 修改时间
     */
    private String modifiedTime;

    private User user;
}
