package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 用户成功登录历史。
 */
@Data
@Table(value = "user_login_history", camelToUnderline = false)
public class UserLoginHistory {
    /** 前台登录 */
    public static final int TYPE_WEB = 1;
    /** 后台登录 */
    public static final int TYPE_ADMIN = 2;

    @Id(keyType = KeyType.Auto)
    private long id;
    private long userId;
    private long loginTime;
    private String ip;
    private String userAgent;
    private int loginType;
}
