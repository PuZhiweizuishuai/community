package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:04
 * 在线用户
 */
@Data
@Table(value = "onlineUser", camelToUnderline = false)
public class OnlineUser {
    @Id(keyType = KeyType.None)
    private long id;

    private String userName;

    private String token;

    private String ip;

    private long time;

    private long expireTime;

    public OnlineUser() {

    }

    public OnlineUser(long id, String userName, String token, String ip, long time, long expireTime) {
        this.id = id;
        this.userName = userName;
        this.token = token;
        this.ip = ip;
        this.time = time;
        this.expireTime = expireTime;
    }
}
