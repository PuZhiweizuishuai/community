package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:28
 * 通知类型
 */
public enum NotificationTypeEnum {
    /**
     * 回复了问题
     */
    REPLY_QUESTION(1, "回复了我的问题"),

    /**
     * 回复了评论
     */
    REPLY_COMMENT(2, "回复了我的评论"),

    /**
     * 点赞了问题
     */
    LIKE_QUESTION(3, "点赞了我的问题"),

    /**
     * 点赞了评论
     */
    LIKE_COMMENT(4, "点赞了我的评论"),

    /**
     * 系统消息
     */
    SYSTEM_NOTIFICATION(5, "系统消息"),


    SYSTEM_DELETE_QUESTION(6, "删除了你的问题"),

    /**
     * 删除评论
     * */
    SYSTEM_DELETE_COMMENT(7, "删除了问题"),

    SYSTEM_RESET_QUESTION(8, "恢复了你的问题"),

    /**
     * 恢复评论
     * */
    SYSTEM_RESET_COMMENT(9, "恢复了问题"),

    /**
     * 内容审核：提交待审
     */
    REVIEW_QUESTION_PENDING(10, "提交了问题审核"),

    REVIEW_COMMENT_PENDING(11, "提交了评论审核"),

    /**
     * 内容审核：通过
     */
    REVIEW_QUESTION_PASS(12, "审核通过了你的问题"),

    REVIEW_COMMENT_PASS(13, "审核通过了你的评论"),

    /**
     * 内容审核：驳回
     */
    REVIEW_QUESTION_REJECT(14, "退回了你的问题"),

    REVIEW_COMMENT_REJECT(15, "退回了你的评论");




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
