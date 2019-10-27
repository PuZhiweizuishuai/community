package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:07
 */
@Data
public class FollowTopicDTO {
    private Long topicId;
    private String topicTitle;
    private String token;
}
