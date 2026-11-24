package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * 用户封禁状态查询结果（users 表封禁字段）。
 */
@Data
public class UserBanStatus {

    private long userId;

    /** 0 正常，1 临时封禁，2 永久封禁 */
    private int banType;

    /** 临时封禁结束时间（毫秒），永久封禁为 0 */
    private long banEndTime;

    /** 最近一次封禁时间（毫秒） */
    private long banTime;

    private String banReason;
}
