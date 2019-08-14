package com.buguagaoshu.community.model;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:04
 * 在线用户
 */
public class OnlineUser {
    private long id;

    private String userName;

    private String token;

    private String ip;

    private String time;

    public long getId() {
        return id;
    }

    public OnlineUser(long id, String userName, String token, String ip, String time) {
        this.id = id;
        this.userName = userName;
        this.token = token;
        this.ip = ip;
        this.time = time;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
