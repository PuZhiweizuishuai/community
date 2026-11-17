package com.buguagaoshu.community.enums;

/**
 * 内容审核状态。
 *
 * <p>与删除状态 status 相互独立：status 表示内容是否被删除，reviewStatus 表示是否允许公开展示。
 * 存量数据在迁移脚本中统一置为 APPROVED，保证未开启审核前的历史内容继续可见。
 */
public enum ReviewStatusEnum {
    /**
     * 待审核：仅作者与管理员可见
     */
    PENDING(0, "待审核"),

    /**
     * 已通过：所有用户可见
     */
    APPROVED(1, "已通过"),

    /**
     * 未通过：仅作者与管理员可见，作者可修改后重新提交
     */
    REJECTED(2, "未通过");

    private final int status;
    private final String name;

    ReviewStatusEnum(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static boolean isExist(int status) {
        for (ReviewStatusEnum item : values()) {
            if (item.status == status) {
                return true;
            }
        }
        return false;
    }

    public static String nameOfStatus(int status) {
        for (ReviewStatusEnum item : values()) {
            if (item.status == status) {
                return item.name;
            }
        }
        return "";
    }
}
