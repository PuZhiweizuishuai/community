package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import lombok.Data;

/**
 * 统一的 API 响应体：{ code, message, data }。
 * code == 200 表示成功，其余为业务错误码。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class ResultDTO<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ResultDTO<T> errorOf(Integer code, String message) {
        ResultDTO<T> resultDTO = new ResultDTO<>();
        resultDTO.setCode(code);
        resultDTO.setMessage(message);
        return resultDTO;
    }

    public static <T> ResultDTO<T> errorOf(CustomizeErrorCode errorCode) {
        return errorOf(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> ResultDTO<T> errorOf(CustomizeException e) {
        return errorOf(e.getCode(), e.getMessage());
    }

    public static <T> ResultDTO<T> okOf() {
        ResultDTO<T> resultDTO = new ResultDTO<>();
        resultDTO.setCode(200);
        resultDTO.setMessage("请求成功");
        return resultDTO;
    }

    public static <T> ResultDTO<T> okOf(T t) {
        ResultDTO<T> resultDTO = new ResultDTO<>();
        resultDTO.setCode(200);
        resultDTO.setMessage("请求成功");
        resultDTO.setData(t);
        return resultDTO;
    }
}
