package com.buguagaoshu.community.enums;

/**
 * 话题可见性。
 */
public enum TopicVisibilityEnum {
    PUBLIC(0, "公开"),
    PRIVATE_APPLY(1, "私有 · 可申请"),
    PRIVATE_HIDDEN(2, "私有 · 仅邀请");

    private final int code;
    private final String name;

    TopicVisibilityEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static boolean isValid(Integer code) {
        return code != null && code >= PUBLIC.code && code <= PRIVATE_HIDDEN.code;
    }

    public static TopicVisibilityEnum of(Integer code) {
        if (code != null && code == PRIVATE_APPLY.code) {
            return PRIVATE_APPLY;
        }
        if (code != null && code == PRIVATE_HIDDEN.code) {
            return PRIVATE_HIDDEN;
        }
        return PUBLIC;
    }
}
