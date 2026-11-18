package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.exception.ICustomizeErrorCode;
import lombok.Data;

/**
 * 统一 API 响应体：{@code { status, message, timestamp, data }}。
 *
 * <ul>
 *   <li>status：HTTP 语义状态码，与响应行状态一致（200 成功，4xx 客户端错误，5xx 服务端错误）</li>
 *   <li>message：面向用户的提示信息</li>
 *   <li>timestamp：响应生成时间（epoch 毫秒）</li>
 *   <li>data：真正的业务数据，成功时才有意义</li>
 * </ul>
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class ResultDTO<T> {
    private Integer status;
    private String message;
    private Long timestamp;
    private T data;

    public static <T> ResultDTO<T> okOf() {
        return of(200, "请求成功", null);
    }

    public static <T> ResultDTO<T> okOf(T data) {
        return of(200, "请求成功", data);
    }

    public static <T> ResultDTO<T> errorOf(ICustomizeErrorCode errorCode) {
        return of(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    public static <T> ResultDTO<T> errorOf(CustomizeException e) {
        return of(e.getStatus(), e.getMessage(), null);
    }

    /**
     * 兼容旧调用：第一个参数既可以是历史业务码（如 2004），也可以是 HTTP 状态码（如 400）。
     */
    public static <T> ResultDTO<T> errorOf(Integer code, String message) {
        return of(CustomizeErrorCode.resolveStatus(code), message, null);
    }

    private static <T> ResultDTO<T> of(int status, String message, T data) {
        ResultDTO<T> resultDTO = new ResultDTO<>();
        resultDTO.setStatus(status);
        resultDTO.setMessage(message);
        resultDTO.setTimestamp(System.currentTimeMillis());
        resultDTO.setData(data);
        return resultDTO;
    }
}
