package com.buguagaoshu.community.exception;

/**
 * 业务错误码。
 *
 * <p>code 为历史内部编码，仅用于日志与兼容；status 为对外响应使用的 HTTP 状态码，
 * 保证 HTTP 状态与响应体 status 一致。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:25
 */
public enum CustomizeErrorCode implements ICustomizeErrorCode {
    /** @deprecated 会暴露「账号是否存在」，改用 {@link #SIGN_IN_CREDENTIALS_ERROR} */
    @Deprecated
    SIGN_IN_EMAIL_ERROR(1001, 401, "登陆账号（邮箱）错误！"),
    /** @deprecated 会暴露「账号是否存在」，改用 {@link #SIGN_IN_CREDENTIALS_ERROR} */
    @Deprecated
    SIGN_IN_PASSWORD_ERROR(1002, 401, "登陆密码错误！"),
    SIGN_IN_CAPTCHA_ERROR(1003, 400, "验证码错误！"),
    SIGN_IN_EMAIL_OR_PASSWORD_NULL(1004, 400, "用户名和密码不能为空"),
    /** 登录失败统一提示：不区分「账号不存在」与「密码错误」，防止账号枚举 */
    SIGN_IN_CREDENTIALS_ERROR(1005, 401, "邮箱或密码错误！"),
    QUESTION_NOT_FOUND(2001, 404, "你找到问题不在了，要不要换个试试？"),
    TARGET_PARAM_NOT_FOUND(2002, 400, "未选中任何问题或评论进行回复"),
    NO_LOGIN(2003, 401, "当前操作需要登录，请登陆后重试"),
    SYS_ERROR(2004, 500, "服务冒烟了，要不然你稍后再试试！！！"),
    TYPE_PARAM_WRONG(2005, 400, "评论类型错误或不存在"),
    COMMENT_NOT_FOUND(2006, 404, "回复的评论不存在了，要不要换个试试？"),
    COMMENT_NO_COMMENT(2013, 404, "该评论暂时没有二级回复！"),
    CONTENT_IS_EMPTY(2007, 400, "输入内容不能为空"),
    READ_NOTIFICATION_FAIL(2008, 403, "你没有读这条信息的权限"),
    NOTIFICATION_NOT_FOUND(2009, 404, "消息莫非是不翼而飞了？"),
    FILE_UPLOAD_FAIL(2010, 500, "图片上传失败"),
    INVALID_INPUT(2011, 400, "非法输入"),
    INVALID_OPERATION(2012, 403, "你正在尝试修改不属于你的问题！"),
    /** 前后端分离后新增的通用错误码 */
    NO_ADMIN(401, 403, "请先登录管理员账号"),
    PARAM_ERROR(400, 400, "请求参数不正确"),
    NOT_FOUND(404, 404, "内容不存在或已被删除"),
    QUESTION_DELETED(2014, 404, "这个问题已经被删除了！如需恢复，请联系管理员！"),
    CAPTCHA_REQUIRED(2015, 400, "验证码错误或已过期，请刷新后重试"),
    TAG_LIMIT(2016, 400, "标签不能为空且最多只能输入 5 个标签"),
    TITLE_INVALID(2017, 400, "标题不能为空，且不能超过 50 个字符"),
    CLASS_INVALID(2018, 400, "请选择问题分类"),
    TOO_MANY_ATTEMPTS(2020, 429, "失败次数过多或操作过于频繁，请 15 分钟后再试"),
    CONTENT_SEGMENT_LIMIT(2021, 400, "内容段落数量必须在 1 ~ 3 个之间"),
    CONTENT_TYPE_INVALID(2022, 400, "内容段落类型不正确"),
    CONTENT_PASSWORD_REQUIRED(2023, 400, "密码可见段落必须设置阅读密码（不超过 64 个字符）"),
    CONTENT_PASSWORD_ERROR(2024, 403, "阅读密码错误"),
    CONTENT_NOT_FOUND(2025, 404, "内容段落不存在"),
    CONTENT_UNDER_REVIEW(2026, 403, "内容正在审核中，暂时无法查看或操作"),
    CONTENT_REVIEW_REJECTED(2027, 403, "内容未通过审核，请修改后重新提交"),
    REVIEW_NOT_FOUND(2028, 404, "待审核内容不存在或已被删除"),
    REVIEW_ALREADY_HANDLED(2029, 409, "该内容已被处理，请刷新后重试"),
    REVIEW_REASON_REQUIRED(2030, 400, "请填写审核不通过原因"),
    REVIEW_RESUBMIT_FORBIDDEN(2031, 403, "该内容当前不可修改，请刷新后重试"),
    USER_BANNED(2032, 403, "账号已被封禁"),
    FORBIDDEN(403, 403, "没有权限执行该操作"),
    CONFLICT(409, 409, "操作冲突，请刷新后重试");

    private final Integer code;
    private final Integer status;
    private final String message;

    CustomizeErrorCode(Integer code, Integer status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Integer getStatus() {
        return status;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    /**
     * 把历史业务码或 HTTP 状态码统一解析为对外 status：
     * 400~599 视为已是 HTTP 状态码；其余按业务码查表；无法识别时按 500 处理。
     */
    public static int resolveStatus(Integer code) {
        if (code == null) {
            return 500;
        }
        if (code >= 400 && code <= 599) {
            return code;
        }
        for (CustomizeErrorCode item : values()) {
            if (item.code.equals(code)) {
                return item.status;
            }
        }
        return 500;
    }
}
