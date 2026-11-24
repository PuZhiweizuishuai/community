package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:28
 * 通知类型
 * <p>
 * 文案约定：文案统一按「收件人视角」书写，且不再拼接发起人，
 * 由前端按 {@link Actor} 决定前缀是「昵称」还是「系统」徽章：
 * <ul>
 *     <li>{@link Actor#USER}：前端渲染为「张三 回复了你的问题」</li>
 *     <li>{@link Actor#SYSTEM}：前端渲染为「[系统] 你的问题已提交审核」</li>
 * </ul>
 * 所以 USER 类型必须是动词短语（谁 + 做了什么），SYSTEM 类型必须是完整的一句话，
 * 不要再出现「系统 提交了问题审核」这种把两段拼在一起读不通的文案。
 */
public enum NotificationTypeEnum {
    /**
     * 回复了问题
     */
    REPLY_QUESTION(1, "回复了你的问题", Actor.USER),

    /**
     * 回复了评论
     */
    REPLY_COMMENT(2, "回复了你的评论", Actor.USER),

    /**
     * 点赞了问题
     */
    LIKE_QUESTION(3, "赞了你的问题", Actor.USER),

    /**
     * 点赞了评论
     */
    LIKE_COMMENT(4, "赞了你的评论", Actor.USER),

    /**
     * 系统消息
     */
    SYSTEM_NOTIFICATION(5, "系统通知", Actor.SYSTEM),

    SYSTEM_DELETE_QUESTION(6, "你的问题已被删除", Actor.SYSTEM),

    /**
     * 删除评论
     * */
    SYSTEM_DELETE_COMMENT(7, "你的评论已被删除", Actor.SYSTEM),

    SYSTEM_RESET_QUESTION(8, "你的问题已恢复", Actor.SYSTEM),

    /**
     * 恢复评论
     * */
    SYSTEM_RESET_COMMENT(9, "你的评论已恢复", Actor.SYSTEM),

    /**
     * 内容审核：提交待审（作者本人提交，系统回执）
     */
    REVIEW_QUESTION_PENDING(10, "你的问题已提交审核", Actor.SYSTEM),

    REVIEW_COMMENT_PENDING(11, "你的评论已提交审核", Actor.SYSTEM),

    /**
     * 内容审核：通过
     */
    REVIEW_QUESTION_PASS(12, "你的问题已通过审核", Actor.SYSTEM),

    REVIEW_COMMENT_PASS(13, "你的评论已通过审核", Actor.SYSTEM),

    /**
     * 内容审核：驳回
     */
    REVIEW_QUESTION_REJECT(14, "你的问题未通过审核", Actor.SYSTEM),

    REVIEW_COMMENT_REJECT(15, "你的评论未通过审核", Actor.SYSTEM),

    /**
     * 管理员删除 / 恢复评论
     */
    ADMIN_DELETE_COMMENT(16, "你的评论已被删除", Actor.SYSTEM),

    ADMIN_RECOVER_COMMENT(17, "你的评论已恢复", Actor.SYSTEM),

    /**
     * 举报处理结果
     */
    REPORT_ACCEPTED(18, "你的举报已受理", Actor.SYSTEM),

    REPORT_REJECTED(19, "你的举报未受理", Actor.SYSTEM),

    /**
     * 标签审核结果
     */
    TAG_REVIEW_PASS(20, "你的标签已通过审核", Actor.SYSTEM),

    TAG_REVIEW_REJECT(21, "你的标签未通过审核", Actor.SYSTEM),

    /**
     * 话题相关通知：outerId 指向话题 ID
     */
    TOPIC_REVIEW_PASS(22, "你创建的话题已通过审核", Actor.SYSTEM),

    TOPIC_REVIEW_REJECT(23, "你创建的话题未通过审核", Actor.SYSTEM),

    /** 吧务收到申请：发起人是申请者本人，需要展示昵称与头像 */
    TOPIC_APPLY_RECEIVED(24, "申请加入你管理的话题", Actor.USER),

    TOPIC_APPLY_PASS(25, "你的加入申请已通过", Actor.SYSTEM),

    TOPIC_APPLY_REJECT(26, "你的加入申请未通过", Actor.SYSTEM),

    /** 邀请：发起人吧主/小吧 */
    TOPIC_INVITE(27, "邀请你加入话题", Actor.USER);


    /**
     * 通知发起人的展示身份
     */
    public enum Actor {
        /**
         * 真实用户触发，前端展示头像 + 昵称
         */
        USER,
        /**
         * 平台 / 管理员触发，前端展示「系统」徽章，不暴露处理人
         */
        SYSTEM
    }

    private final int type;
    private final String name;
    private final Actor actor;


    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Actor getActor() {
        return actor;
    }

    /**
     * 系统文案：前端只渲染文案本身，不再拼「系统」前缀
     */
    public boolean isSystemActor() {
        return actor == Actor.SYSTEM;
    }

    /**
     * 详情是否是系统生成的说明文字（而不是用户原文）。
     * 只有这类详情才做去重与截断，回复 / 点赞的用户原文必须原样保留。
     */
    public boolean isGeneratedDetail() {
        return actor == Actor.SYSTEM || this == TOPIC_INVITE || this == TOPIC_APPLY_RECEIVED;
    }

    NotificationTypeEnum(int status, String name, Actor actor) {
        this.type = status;
        this.name = name;
        this.actor = actor;
    }

    /**
     * 按类型码取枚举，未知类型返回 null（历史脏数据不抛异常）
     */
    public static NotificationTypeEnum typeOf(int type) {
        for (NotificationTypeEnum item : values()) {
            if (item.type == type) {
                return item;
            }
        }
        return null;
    }

    public static String nameOfType(int type) {
        NotificationTypeEnum item = typeOf(type);
        return item == null ? "" : item.getName();
    }

    public static Actor actorOf(int type) {
        NotificationTypeEnum item = typeOf(type);
        // 未知类型按系统消息处理，避免暴露内部处理人
        return item == null ? Actor.SYSTEM : item.getActor();
    }

    public static boolean isSystemActor(int type) {
        return actorOf(type) == Actor.SYSTEM;
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
