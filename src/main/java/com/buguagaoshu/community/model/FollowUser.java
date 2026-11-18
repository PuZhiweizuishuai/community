package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:40
 */
@Data
@Table(value = "follow_users", camelToUnderline = false)
public class FollowUser {
    @Id(keyType = KeyType.Auto)
    private long id;
    private long followUserId;
    private long userId;
    private long createTime;
}
