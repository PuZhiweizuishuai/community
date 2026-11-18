package com.buguagaoshu.community.enums;

/**
 * 举报理由：使用稳定编码入库，展示文案由后端统一返回，避免前后端文案漂移。
 */
public enum ReportReasonEnum {
    SPAM("spam", "垃圾广告"),
    ABUSE("abuse", "辱骂攻击"),
    PORN("porn", "色情低俗"),
    ILLEGAL("illegal", "违法违规"),
    OTHER("other", "其他");

    private final String code;
    private final String name;

    ReportReasonEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ReportReasonEnum of(String code) {
        for (ReportReasonEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    public static String nameOf(String code) {
        ReportReasonEnum item = of(code);
        return item == null ? "" : item.name;
    }
}
