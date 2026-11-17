package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * 问题详情返回的内容段落。locked=true 时 content 为 null，
 * 密码段落需调解锁接口，回复可见段落需评论后重新加载详情。
 */
@Data
public class QuestionContentDto {
    private long contentId;
    private int sort;
    private int type;
    private String content;
    private boolean locked;
}