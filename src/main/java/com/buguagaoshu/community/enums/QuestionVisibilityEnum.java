package com.buguagaoshu.community.enums;

/**
 * 帖子可见性。
 */
public enum QuestionVisibilityEnum {
    PUBLIC(0, "公开"),
    LOGIN_REQUIRED(1, "登录可见");

    private final int code;
    private final String name;

    QuestionVisibilityEnum(int code, String name) {
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
        return code != null && (code == PUBLIC.code || code == LOGIN_REQUIRED.code);
    }

    public static QuestionVisibilityEnum of(Integer code) {
        return code != null && code == LOGIN_REQUIRED.code ? LOGIN_REQUIRED : PUBLIC;
    }
}
