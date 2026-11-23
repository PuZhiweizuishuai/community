package com.buguagaoshu.community.enums;

/**
 * 内容审核对象类型。前端与接口参数统一使用小写字符串，避免暴露数据库表名。
 */
public enum ReviewTargetTypeEnum {
    QUESTION("question", "问题"),
    COMMENT("comment", "评论"),
    TOPIC("topic", "话题");

    private final String type;
    private final String name;

    ReviewTargetTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static boolean isExist(String type) {
        for (ReviewTargetTypeEnum item : values()) {
            if (item.type.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    public static ReviewTargetTypeEnum of(String type) {
        for (ReviewTargetTypeEnum item : values()) {
            if (item.type.equalsIgnoreCase(type)) {
                return item;
            }
        }
        return null;
    }
}
