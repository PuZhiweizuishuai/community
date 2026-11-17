package com.buguagaoshu.community.enums;

/**
 * 问题内容段落类型
 */
public enum QuestionContentTypeEnum {
    /**
     * 普通文本，直接可见
     */
    NORMAL(0),

    /**
     * 密码可见，输入正确阅读密码后可见
     */
    PASSWORD(1),

    /**
     * 回复可见，评论过该问题后可见
     */
    REPLY(2);

    private final Integer type;

    QuestionContentTypeEnum(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public static boolean isExist(Integer type) {
        for (QuestionContentTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}