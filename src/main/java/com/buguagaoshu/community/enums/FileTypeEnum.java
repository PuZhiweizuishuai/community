package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-20 14:26
 */
public enum FileTypeEnum {
    /**
     * 文件类型
     * */
    FILE(0, "文件"),

    IMAGE_FILE(1, "图片"),

    VIDEO_FILE(2, "视频"),

    MUSIC_FILE(3, "声音");

    int code;
    String type;
    FileTypeEnum(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
