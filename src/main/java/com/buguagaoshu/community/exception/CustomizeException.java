package com.buguagaoshu.community.exception;

/**
 * 业务异常。
 *
 * <p>status 为对外响应的 HTTP 状态码，始终与响应体中的 status 保持一致；
 * code 为历史内部编码，仅用于日志与兼容。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:22
 */
public class CustomizeException extends RuntimeException {
    private final String message;
    private final Integer code;
    private final Integer status;

    public CustomizeException(ICustomizeErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus();
        this.message = errorCode.getMessage();
    }

    public CustomizeException(Integer code, String message) {
        this.code = code;
        this.status = CustomizeErrorCode.resolveStatus(code);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }

    public Integer getStatus() {
        return status;
    }
}
