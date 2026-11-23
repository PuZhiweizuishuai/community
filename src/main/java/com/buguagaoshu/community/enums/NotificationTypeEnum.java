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

    REVIEW_COMMENT_REJECT(15, "退回了你的评论"),

    /**
     * 管理员删除 / 恢复评论
     */
    ADMIN_DELETE_COMMENT(16, "删除了你的评论"),

    ADMIN_RECOVER_COMMENT(17, "恢复了你的评论"),

    /**
     * 举报处理结果
     */
    REPORT_ACCEPTED(18, "举报已受理"),

    REPORT_REJECTED(19, "举报未受理"),

    /**
     * 标签审核结果
     */
    TAG_REVIEW_PASS(20, "标签审核通过"),

    TAG_REVIEW_REJECT(21, "标签审核未通过"),

    /**
     * 话题相关通知：outerId 指向话题 ID
     */
    TOPIC_REVIEW_PASS(22, "话题审核通过"),

    TOPIC_REVIEW_REJECT(23, "话题审核未通过"),

    TOPIC_APPLY_RECEIVED(24, "收到加入申请"),

    TOPIC_APPLY_PASS(25, "加入申请已通过"),

    TOPIC_APPLY_REJECT(26, "加入申请未通过"),

    TOPIC_INVITE(27, "邀请你加入话题");


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

    /** 话题通知：outerId 语义为话题 ID，而不是问题 ID */
    public static boolean isTopicType(int type) {
        return type >= TOPIC_REVIEW_PASS.type && type <= TOPIC_INVITE.type;
    }

    /** 标签审核通知：没有可跳转的详情页，outerId 为占位值 */
    public static boolean isTagType(int type) {
        return type == TAG_REVIEW_PASS.type || type == TAG_REVIEW_REJECT.type;
    }
}
