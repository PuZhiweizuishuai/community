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
    INVALID_OPERATION(2012, "兄弟，是不是走错房间了？"),;

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
