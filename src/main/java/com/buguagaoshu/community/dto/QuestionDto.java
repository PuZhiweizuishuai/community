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
    private String tag;
    private String createTime;
    private String alterTime;
    private int status;
    private User user;
}
