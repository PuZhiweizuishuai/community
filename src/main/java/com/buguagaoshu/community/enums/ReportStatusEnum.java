package com.buguagaoshu.community.enums;

/**
 * 举报状态：0 待处理，1 已受理，2 已驳回。
 */
public enum ReportStatusEnum {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已受理"),
    REJECTED(2, "已驳回");

    private final int status;
    private final String name;

    ReportStatusEnum(int status, String name) {
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
        for (ReportStatusEnum item : values()) {
            if (item.status == status) {
                return true;
            }
        }
        return false;
    }

    public static String nameOf(int status) {
        for (ReportStatusEnum item : values()) {
            if (item.status == status) {
                return item.name;
            }
        }
        return "";
    }
}
