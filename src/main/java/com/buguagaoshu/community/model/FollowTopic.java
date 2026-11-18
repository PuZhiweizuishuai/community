package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:04
 * 关注话题
 */
@Data
@Table(value = "follow_topics", camelToUnderline = false)
public class FollowTopic {
    @Id(keyType = KeyType.Auto)
    private long followTopicId;
    private long topicId;
    private String topicTitle;
    private String topicImage;
    private long userId;
    private long createTime;
}
