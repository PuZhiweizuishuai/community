package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:40
 */
@Data
public class FollowUser {
    private long id;
    private long followUserId;
    private long userId;
    private long createTime;
}
