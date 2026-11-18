package com.buguagaoshu.community.constant;

/**
 * 权限编码常量。与 V12 / V13 迁移脚本中的 sys_permission.code 保持一致。
 */
public final class PermissionCodes {

    /** 后台基础访问权限：所有后台接口的准入前提 */
    public static final String ADMIN_ACCESS = "admin:access";
    public static final String ADMIN_USER = "admin:user";
    public static final String ADMIN_USER_ROLE = "admin:user:role";
    public static final String ADMIN_USER_BAN = "admin:user:ban";
    public static final String ADMIN_QUESTION = "admin:question";
    public static final String ADMIN_REVIEW = "admin:review";
    public static final String ADMIN_SEARCH = "admin:search";
    public static final String ADMIN_TOPIC = "admin:topic";
    public static final String ADMIN_CLASSIFICATION = "admin:classification";
    public static final String ADMIN_ADVERTISEMENT = "admin:advertisement";
    public static final String ADMIN_FILE = "admin:file";
    public static final String ADMIN_STORAGE = "admin:storage";
    public static final String ADMIN_SYSTEM = "admin:system";
    public static final String ADMIN_ROLE = "admin:role";

    /** 社区基础权限（普通用户） */
    public static final String CONTENT_QUESTION_PUBLISH = "content:question:publish";
    public static final String CONTENT_COMMENT_CREATE = "content:comment:create";
    public static final String FILE_UPLOAD = "file:upload";

    private PermissionCodes() {
    }
}
