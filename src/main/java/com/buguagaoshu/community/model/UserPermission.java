package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:04
 * 用户权限
 */
@Data
public class UserPermission {
    private long id;
    /**
     * 权限
     * 0 最高权限即管理员
     * 1 普通用户
     * 2 vip
     * */
    private int power;

    /**
     * 修改人
     * 0 为初始创建
     * */
    private String modifier;

    /**
     * 修改时间
     * */
    private String updateTime;

    /**
     * Vip 到期时间
     * */
    private long dueTime;

    public UserPermission() {

    }

    public UserPermission(long id, int power, String modifier, String updateTime, long dueTime) {
        this.id = id;
        this.power = power;
        this.modifier = modifier;
        this.updateTime = updateTime;
        this.dueTime = dueTime;
    }
}
