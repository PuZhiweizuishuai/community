package com.buguagaoshu.community.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 18:48
 */
public enum  TagClassTypeEnum {
    /**
     * 开发语言
     * */
    DEVELOPMENT_LANGUAGE(1, "开发语言"),

    PLATFORM_FRAMEWORK(2, "平台框架"),

    SERVER(3, "服务器"),

    DATABASE(4, "数据库"),

    DEVELOPMENT_TOOL(5, "开发工具"),

    SYSTEM_DEVICE(6, "系统设备"),

    LEISURE(7, "休闲灌水");


    int typeCode;

    String typeName;

    TagClassTypeEnum(int typeCode, String typeName) {
        this.typeCode = typeCode;
        this.typeName = typeName;
    }
}
