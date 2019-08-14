package com.buguagaoshu.community.model;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:04
 * 用户权限
 */
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

    public UserPermission() {

    }

    public UserPermission(long id, int power, String modifier, String updateTime) {
        this.id = id;
        this.power = power;
        this.modifier = modifier;
        this.updateTime = updateTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifer) {
        this.modifier = modifier;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
