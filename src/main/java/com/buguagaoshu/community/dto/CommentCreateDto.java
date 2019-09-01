package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:22
 * 接收用户评论
 */
@Data
public class CommentCreateDto {
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
     * 内容
     * */
    private String content;


    /**
     * 验证码
     * */
    private String captcha;
}
