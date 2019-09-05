package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:04
 * 在线用户
 */
@Data
public class OnlineUser {
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
