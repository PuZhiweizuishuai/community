package com.buguagaoshu.community.exception;

/**
 * 业务错误定义：message 面向用户，status 为对外统一使用的 HTTP 状态码。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:25
 */
public interface ICustomizeErrorCode {
    /**
     * 返回错误信息
     *
     * @return 错误信息
     */
    String getMessage();

    /**
     * 返回对外响应状态码（HTTP 语义）
     *
     * @return 状态码
     */
    Integer getStatus();

    /**
     * 返回内部错误代码，仅用于日志与兼容旧代码，不再下发给前端
     *
     * @return 内部错误代码
     */
    Integer getCode();
}
