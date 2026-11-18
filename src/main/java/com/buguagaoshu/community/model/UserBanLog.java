package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 用户封禁流水。状态为 1 的记录表示当前生效的封禁，解封时置为 0。
 */
@Data
@Table(value = "user_ban_logs", camelToUnderline = false)
public class UserBanLog {

    @Id(keyType = KeyType.Auto)
    private long id;

    private long userId;

    /** 1 临时封禁，2 永久封禁 */
    private int banType;

    /** 临时封禁结束时间，永久封禁为 0 */
    private long endTime;

    private String reason;

    private long operatorId;

    private long createTime;

    /** 1 生效中，0 已解除 */
    private int status;

    private Long liftedTime;

    private Long liftOperatorId;
}
