package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 14:29
 */
@Data
public class FollowQuestion {
    private long id;
    private long followQuestionId;
    private long followQuestionAuthor;
    private String followQuestionTitle;
    private long userId;
    private long createTime;
    private String time;
}
