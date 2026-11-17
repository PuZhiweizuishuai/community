package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.model.User;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 22:40
 */
@Data
public class QuestionDto {
    private long questionId;
    private long userId;
    private String title;
    private String classification;
    private String description;
    private String fileUrl;
    private long viewCount;
    private long commentCount;
    private long likeCount;
    private long followCount;
    private String tag;
    private String createTime;
    private String alterTime;
    private int status;
    private int reviewStatus = 1;
    private String reviewReason;
    private String reviewSuggestion;
    private long reviewTime;
    private User user;

    /**
     * 列表场景瘦身：去掉正文，作者只保留列表渲染需要的字段，
     * 避免首页数据包携带 Markdown 正文与用户详细资料（含邮箱等敏感信息）。
     */
    public QuestionDto toListItem() {
        this.description = null;
        if (user != null) {
            User author = new User();
            author.setId(user.getId());
            author.setUserId(user.getUserId());
            author.setUserName(user.getUserName());
            author.setHeadUrl(user.getHeadUrl());
            this.user = author;
        }
        return this;
    }
}
