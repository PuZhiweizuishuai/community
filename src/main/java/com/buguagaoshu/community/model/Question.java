package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:44
 */
@Data
public class Question {
    private long questionId = -1;
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
    private long createTime;
    private long alterTime;

    /**
     * 状态，删除 或 没删除
     * */
    private int status;
}
