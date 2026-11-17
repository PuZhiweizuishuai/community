package com.buguagaoshu.community.exception;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:25
 * 异常代码
 */
public enum CustomizeErrorCode implements ICustomizeErrorCode {
    SIGN_IN_EMAIL_ERROR(1001, "登陆账号（邮箱）错误！"),
    SIGN_IN_PASSWORD_ERROR(1002, "登陆密码错误！"),
    SIGN_IN_CAPTCHA_ERROR(1003, "验证码错误！"),
    SIGN_IN_EMAIL_OR_PASSWORD_NULL(1004, "用户名和密码不能为空"),
    QUESTION_NOT_FOUND(2001, "你找到问题不在了，要不要换个试试？"),
    TARGET_PARAM_NOT_FOUND(2002, "未选中任何问题或评论进行回复"),
    NO_LOGIN(2003, "当前操作需要登录，请登陆后重试"),
    SYS_ERROR(2004, "服务冒烟了，要不然你稍后再试试！！！"),
    TYPE_PARAM_WRONG(2005, "评论类型错误或不存在"),
    COMMENT_NOT_FOUND(2006, "回复的评论不存在了，要不要换个试试？"),
    COMMENT_NO_COMMENT(2013, "该评论暂时没有二级回复！"),
    CONTENT_IS_EMPTY(2007, "输入内容不能为空"),
    READ_NOTIFICATION_FAIL(2008, "你没有读这条信息的权限"),
    NOTIFICATION_NOT_FOUND(2009, "消息莫非是不翼而飞了？"),
    FILE_UPLOAD_FAIL(2010, "图片上传失败"),
    INVALID_INPUT(2011, "非法输入"),
    INVALID_OPERATION(2012, "你正在尝试修改不属于你的问题！"),
    /** 前后端分离后新增的通用错误码 */
    NO_ADMIN(401, "请先登录管理员账号"),
    PARAM_ERROR(400, "请求参数不正确"),
    NOT_FOUND(404, "内容不存在或已被删除"),
    QUESTION_DELETED(2014, "这个问题已经被删除了！如需恢复，请联系管理员！"),
    CAPTCHA_REQUIRED(2015, "验证码错误或已过期，请刷新后重试"),
    TAG_LIMIT(2016, "标签不能为空且最多只能输入 6 个标签"),
    TITLE_INVALID(2017, "标题不能为空，且不能超过 50 个字符"),
    CLASS_INVALID(2018, "请选择问题分类"),
    TOO_MANY_ATTEMPTS(2020, "失败次数过多或操作过于频繁，请 15 分钟后再试"),
    CONTENT_SEGMENT_LIMIT(2021, "内容段落数量必须在 1 ~ 3 个之间"),
    CONTENT_TYPE_INVALID(2022, "内容段落类型不正确"),
    CONTENT_PASSWORD_REQUIRED(2023, "密码可见段落必须设置阅读密码（不超过 64 个字符）"),
    CONTENT_PASSWORD_ERROR(2024, "阅读密码错误"),
    CONTENT_NOT_FOUND(2025, "内容段落不存在"),
    CONTENT_UNDER_REVIEW(2026, "内容正在审核中，暂时无法查看或操作"),
    CONTENT_REVIEW_REJECTED(2027, "内容未通过审核，请修改后重新提交"),
    REVIEW_NOT_FOUND(2028, "待审核内容不存在或已被删除"),
    REVIEW_ALREADY_HANDLED(2029, "该内容已被处理，请刷新后重试"),
    REVIEW_REASON_REQUIRED(2030, "请填写审核不通过原因"),
    REVIEW_RESUBMIT_FORBIDDEN(2031, "该内容当前不可修改，请刷新后重试"),
    FORBIDDEN(403, "没有权限执行该操作"),
    CONFLICT(409, "操作冲突，请刷新后重试");

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    private Integer code;
    private String message;

    CustomizeErrorCode(Integer code, String message) {
        this.message = message;
        this.code = code;
    }
}
