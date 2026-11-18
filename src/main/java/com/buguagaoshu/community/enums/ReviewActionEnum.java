package com.buguagaoshu.community.enums;

/**
 * 审核流水动作，写入 content_review_logs 便于追溯。
 */
public enum ReviewActionEnum {
    SUBMIT(1, "提交审核"),
    APPROVE(2, "审核通过"),
    REJECT(3, "审核驳回");

    private final int action;
    private final String name;

    ReviewActionEnum(int action, String name) {
        this.action = action;
        this.name = name;
    }

    public int getAction() {
        return action;
    }

    public String getName() {
        return name;
    }
}
