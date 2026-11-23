package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.model.User;
import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 22:40
 */
@Data
public class QuestionDto {
    private long questionId;
    private long userId;
    private String title;
    /** 问题类型 code（classifications.kind=question） */
    private Integer classification;
    /** 所属话题 */
    private Long topicId;
    private String topicTitle;
    private int isTop;
    private int isEssence;
    /** 可见性：0 公开，1 登录可见 */
    private int visibility;
    /** 已通过审核的标签（列表批量装配；详情页作者本人额外可见自己的待审核标签） */
    private List<QuestionTagDto> tags;
    private String description;
    private String fileUrl;
    private long viewCount;
    private long commentCount;
    private long likeCount;
    private long followCount;
    private String tag;
    /** 创建时间（毫秒时间戳，展示格式由前端决定） */
    private long createTime;
    /** 最后修改时间（毫秒时间戳，展示格式由前端决定） */
    private long alterTime;
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
