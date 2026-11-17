package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
     */
    @Min(value = 1, message = "问题 ID 不合法")
    private long questionId;

    /**
     * 父级评论
     * */
    private long parentCommentId;

    /**
     * 评论目标 id
     */
    @Min(value = 1, message = "评论目标不合法")
    private long parentId;

    /**
     * 评论类型
     */
    private int type;


    /**
     * 内容
     */
    @NotBlank(message = "输入内容不能为空")
    @Size(max = 5000, message = "评论内容过长")
    private String content;


    /**
     * 验证码
     */
    private String captcha;
}
