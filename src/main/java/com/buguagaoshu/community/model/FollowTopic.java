package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:04
 * 关注话题
 */
@Data
public class FollowTopic {
    private long followTopicId;
    private long topicId;
    private String topicTitle;
    private String topicImage;
    private long userId;
    private long createTime;
}
