package com.buguagaoshu.community.enums;

/**
 * 话题成员来源。
 */
public enum TopicJoinTypeEnum {
    APPLY("apply"),
    INVITE("invite");

    private final String code;

    TopicJoinTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
