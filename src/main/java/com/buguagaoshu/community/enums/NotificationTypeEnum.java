package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:28
 * 通知类型
 */
public enum NotificationTypeEnum {
    /**
     * 回复了问题
     * */
    REPLY_QUESTION(1, "回复了问题"),

    /**
     * 回复了评论
     * */
    REPLY_COMMENT(2, "回复了你在问题"),

    /**
     * 点赞了问题
     * */
    LIKE_QUESTION(3, "点赞了问题"),

    /**
     * 点赞了评论
     * */
    LIKE_COMMENT(4, "点赞了评论"),

    /**
     * 系统消息
     * */
    SYSTEM_NOTIFICATION(5, "系统消息");


    private int type;
    private String name;


    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    NotificationTypeEnum(int status, String name) {
        this.type = status;
        this.name = name;
    }

    public static String nameOfType(int type) {
        for (NotificationTypeEnum notificationTypeEnum : NotificationTypeEnum.values()) {
            if (notificationTypeEnum.getType() == type) {
                return notificationTypeEnum.getName();
            }
        }
        return "";
    }
}
