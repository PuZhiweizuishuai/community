package com.buguagaoshu.community.enums;

/**
 * 话题成员状态。
 */
public enum TopicMemberStatusEnum {
    PENDING(0, "待处理"),
    APPROVED(1, "已加入"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String name;

    TopicMemberStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
