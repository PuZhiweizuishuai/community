package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:02
 * 点赞类型
 */
public enum ClickLikeTypeEnum {
    /**
     * 点赞问题
     * */
    LIKE_QUESTION(1, "点赞了你的问题"),


    /**
     * 点赞回复
     * */
    LIKE_COMMENT(2, "点赞了你的回复"),

    LIKE_QUESTION_NOT_FOUND(-1, "点赞的问题不存在！"),

    LIKE_COMMENT_NOT_FOUND(-2, "点赞的评论不存在了！"),

    REPEAT_LIKE(-3, "你已经点过赞了，不能重复点赞，取消点赞功能开发中!"),

    SUCCESS(0, "点赞成功!"),

    SUCCESS_CANCEL(3, "取消点赞成功！");


    int type;

    String name;

    ClickLikeTypeEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getName(int type) {
        for (ClickLikeTypeEnum clickLikeTypeEnum : ClickLikeTypeEnum.values()) {
            if(clickLikeTypeEnum.getType() == type) {
                return clickLikeTypeEnum.getName();
            }
        }
        return "未知错误，请重试！";
    }
}
