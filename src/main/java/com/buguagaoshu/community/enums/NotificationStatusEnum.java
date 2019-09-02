package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:26
 * 通知状态
 */
public enum  NotificationStatusEnum {
    /**
     * 未读
     * */
    UNREAD(0),

    /**
     * 已读
     */
    READ(1);

    private int status;

    public int getStatus() {
        return status;
    }

    NotificationStatusEnum(int status) {
        this.status = status;
    }
}
